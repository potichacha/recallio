package com.recallio.runner.sandbox;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jboss.logging.Logger;

/**
 * Exécute une soumission dans un conteneur jetable (ADR 0030), lancé avec la ligne de commande {@code docker} : les
 * fichiers entrent par l'entrée standard (une archive tar), le résultat sort par la sortie standard.
 *
 * <p>Le conteneur n'a ni réseau, ni système de fichiers inscriptible hors de deux {@code tmpfs}, ni capacités, ni
 * droits root ; sa mémoire, son processeur et son nombre de processus sont plafonnés, et il est tué au bout du délai.
 */
@ApplicationScoped
public class DockerSandbox {

    private static final Logger LOG = Logger.getLogger(DockerSandbox.class);

    static final String REPORT = "@@recallio:report";
    static final String COMPILATION_ERROR = "@@recallio:compilation-error";

    /** Au-delà, la sortie est tronquée : un rapport de tests honnête est bien plus court. */
    static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    static final int MAX_COMPILATION_OUTPUT = 10_000;
    private static final Duration KILL_TIMEOUT = Duration.ofSeconds(10);
    private static final int FILE_MODE = 0644;

    private final Map<Language, String> images;
    private final Duration timeout;
    private final Optional<String> runtime;

    @Inject
    DockerSandbox(SandboxConfig config) {
        this(
                Map.of(Language.JAVA, config.image(Language.JAVA), Language.GO, config.image(Language.GO)),
                config.timeout(),
                config.dockerRuntime());
    }

    /** @param images l'image de chaque langage accepté ; une soumission d'un autre langage est une erreur du runner */
    public DockerSandbox(Map<Language, String> images, Duration timeout, Optional<String> runtime) {
        this.images = Map.copyOf(images);
        this.timeout = timeout;
        this.runtime = runtime;
    }

    public RunResult run(Submission submission) {
        String image = images.get(submission.language());
        if (image == null) {
            LOG.errorf("Aucune image pour le langage %s", submission.language());
            return RunResult.failure(Status.ERROR, "Le bac à sable est indisponible.");
        }
        String name = "recallio-run-" + UUID.randomUUID();
        Process process;
        try {
            process = new ProcessBuilder(command(name, image)).start();
        } catch (IOException e) {
            LOG.errorf(e, "Impossible de lancer docker");
            return RunResult.failure(Status.ERROR, "Le bac à sable est indisponible.");
        }
        try (ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = readers.submit(() -> read(process.getInputStream()));
            Future<String> stderr = readers.submit(() -> read(process.getErrorStream()));
            send(submission, process.getOutputStream());
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                kill(name, process);
                return RunResult.failure(
                        Status.TIMEOUT, "L'exécution a dépassé %d secondes.".formatted(timeout.toSeconds()));
            }
            RunResult result = interpret(submission.language(), stdout.get());
            if (result.status() == Status.ERROR) {
                LOG.warnf("Exécution %s en erreur (code %d) : %s", name, process.exitValue(), stderr.get());
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            kill(name, process);
            return RunResult.failure(Status.ERROR, "L'exécution a été interrompue.");
        } catch (ExecutionException e) {
            LOG.errorf(e, "Lecture de la sortie de %s impossible", name);
            return RunResult.failure(Status.ERROR, "Le bac à sable est indisponible.");
        }
    }

    /** La ligne de commande, isolation comprise ; chaque option compte (ADR 0030). */
    List<String> command(String name, String image) {
        List<String> command = new ArrayList<>(List.of(
                "docker",
                "run",
                "--rm",
                "--interactive",
                "--name",
                name,
                "--network",
                "none",
                "--read-only",
                "--tmpfs",
                "/work:rw,exec,size=32m,mode=1777",
                "--tmpfs",
                "/tmp:rw,exec,size=96m,mode=1777",
                "--memory",
                "512m",
                "--memory-swap",
                "512m",
                "--cpus",
                "1",
                "--pids-limit",
                "128",
                "--ulimit",
                "nofile=1024:1024",
                "--cap-drop",
                "ALL",
                "--security-opt",
                "no-new-privileges",
                "--user",
                "65534:65534"));
        runtime.ifPresent(value -> command.addAll(List.of("--runtime", value)));
        command.add(image);
        return command;
    }

    /**
     * Lit la sortie du conteneur. Une sortie qui commence par le marqueur du rapport donne le résultat de chaque test,
     * dans le format du langage (JUnit en XML, {@code test2json} pour Go) ; un rapport sans test veut dire que les tests
     * ne sont pas allés au bout.
     */
    static RunResult interpret(Language language, String stdout) {
        int newline = stdout.indexOf('\n');
        String marker = (newline < 0 ? stdout : stdout.substring(0, newline)).strip();
        String rest = newline < 0 ? "" : stdout.substring(newline + 1);
        if (marker.equals(COMPILATION_ERROR)) {
            return RunResult.failure(Status.COMPILATION_ERROR, truncate(rest.strip()));
        }
        if (!marker.equals(REPORT)) {
            return RunResult.failure(Status.ERROR, "Le bac à sable n'a pas répondu comme prévu.");
        }
        List<TestOutcome> tests;
        try {
            tests = rest.isBlank() ? List.of() : report(language, rest);
        } catch (IllegalArgumentException e) {
            return RunResult.failure(Status.ERROR, "Le rapport des tests est illisible.");
        }
        if (tests.isEmpty()) {
            return RunResult.failure(
                    Status.FAILED,
                    "Les tests ne sont pas allés au bout : le programme s'est arrêté (%s ?) ou a manqué de mémoire."
                            .formatted(language == Language.GO ? "os.Exit" : "System.exit"));
        }
        return RunResult.of(tests);
    }

    private static List<TestOutcome> report(Language language, String report) {
        return switch (language) {
            case JAVA -> JunitReport.parse(report);
            case GO -> GoTestReport.parse(report);
        };
    }

    /** Envoie les fichiers en archive tar : le code dans {@code src/}, les tests dans {@code tests/}. */
    private static void send(Submission submission, OutputStream stdin) {
        try (var tar = new TarArchiveOutputStream(stdin, StandardCharsets.UTF_8.name())) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            add(tar, "src/", submission.code());
            add(tar, "tests/", submission.tests());
        } catch (IOException e) {
            // Le conteneur s'est arrêté avant d'avoir tout lu : sa sortie dira pourquoi.
            LOG.debugf(e, "Envoi de la soumission interrompu");
        }
    }

    private static void add(TarArchiveOutputStream tar, String directory, List<SourceFile> files) throws IOException {
        for (SourceFile file : files) {
            byte[] content = file.content().getBytes(StandardCharsets.UTF_8);
            var entry = new TarArchiveEntry(directory + file.path());
            entry.setSize(content.length);
            entry.setMode(FILE_MODE);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
    }

    /** Lit tout le flux pour ne pas bloquer le conteneur, mais n'en garde que {@link #MAX_OUTPUT_BYTES}. */
    private static String read(InputStream stream) throws IOException {
        var kept = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            int room = MAX_OUTPUT_BYTES - kept.size();
            if (room > 0) {
                kept.write(buffer, 0, Math.min(count, room));
            }
        }
        return kept.toString(StandardCharsets.UTF_8);
    }

    private static void kill(String name, Process process) {
        try {
            new ProcessBuilder("docker", "kill", name)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(KILL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            LOG.warnf(e, "Impossible de tuer le conteneur %s", name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        process.destroyForcibly();
    }

    private static String truncate(String text) {
        return text.length() <= MAX_COMPILATION_OUTPUT ? text : text.substring(0, MAX_COMPILATION_OUTPUT) + "\n…";
    }
}
