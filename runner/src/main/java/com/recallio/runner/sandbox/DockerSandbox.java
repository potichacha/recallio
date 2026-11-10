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
import java.util.Base64;
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
 * Exécute une soumission (ADR 0030) ou un programme du Tour (ADR 0037) dans un conteneur jetable, lancé avec la ligne
 * de commande {@code docker} : les fichiers entrent par l'entrée standard (une archive tar), le résultat sort par la
 * sortie standard.
 *
 * <p>Le conteneur n'a ni réseau, ni système de fichiers inscriptible hors de deux {@code tmpfs}, ni capacités, ni
 * droits root ; sa mémoire, son processeur et son nombre de processus sont plafonnés, et il est tué au bout du délai.
 */
@ApplicationScoped
public class DockerSandbox {

    private static final Logger LOG = Logger.getLogger(DockerSandbox.class);

    static final String REPORT = "@@recallio:report";
    static final String COMPILATION_ERROR = "@@recallio:compilation-error";
    static final String PROGRAM = "@@recallio:program";

    /** Le premier argument de l'image : juger une soumission par ses tests, ou lancer un programme. */
    private static final String TESTS_MODE = "tests";

    private static final String PROGRAM_MODE = "program";

    /** Le code de sortie de {@code timeout} quand le programme a dépassé son délai. */
    static final int TIMEOUT_EXIT_CODE = 124;

    /** Au-delà, la sortie est tronquée : un rapport de tests honnête est bien plus court. */
    static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    static final int MAX_COMPILATION_OUTPUT = 10_000;
    private static final String UNAVAILABLE = "Le bac à sable est indisponible.";
    private static final String UNEXPECTED = "Le bac à sable n'a pas répondu comme prévu.";
    private static final Duration KILL_TIMEOUT = Duration.ofSeconds(10);
    private static final int FILE_MODE = 0644;

    private final Map<Language, String> images;
    private final Duration timeout;
    private final Duration programTimeout;
    private final Optional<String> runtime;

    @Inject
    DockerSandbox(SandboxConfig config) {
        this(
                Map.of(
                        Language.JAVA,
                        config.image(Language.JAVA),
                        Language.GO,
                        config.image(Language.GO),
                        Language.CPP,
                        config.image(Language.CPP)),
                config.timeout(),
                config.programTimeout(),
                config.dockerRuntime());
    }

    /**
     * @param images l'image de chaque langage accepté ; une soumission d'un autre langage est une erreur du runner
     * @param timeout le délai du conteneur, compilation comprise
     * @param programTimeout le délai d'un programme une fois compilé, en secondes entières, plus court que
     *     {@code timeout}
     */
    public DockerSandbox(
            Map<Language, String> images, Duration timeout, Duration programTimeout, Optional<String> runtime) {
        if (programTimeout.toSeconds() < 1) {
            // timeout 0 ne limiterait plus rien.
            throw new IllegalArgumentException("Le délai d'un programme est d'au moins une seconde");
        }
        this.images = Map.copyOf(images);
        this.timeout = timeout;
        this.programTimeout = programTimeout;
        this.runtime = runtime;
    }

    public RunResult run(Submission submission) {
        Outcome outcome = launch(submission.language(), List.of(TESTS_MODE), tar -> {
            add(tar, "src/", submission.code());
            add(tar, "tests/", submission.tests());
        });
        return switch (outcome) {
            case Finished finished -> {
                RunResult result = interpret(submission.language(), finished.stdout());
                if (result.status() == Status.ERROR) {
                    finished.logUnexpected();
                }
                yield result;
            }
            case Unfinished unfinished ->
                RunResult.failure(unfinished.timedOut() ? Status.TIMEOUT : Status.ERROR, unfinished.message());
        };
    }

    public ProgramResult run(Program program) {
        Outcome outcome = launch(
                program.language(),
                List.of(PROGRAM_MODE, String.valueOf(programTimeout.toSeconds())),
                tar -> add(tar, "src/", program.files()));
        return switch (outcome) {
            case Finished finished -> {
                ProgramResult result = interpretProgram(finished.stdout(), programTimeout);
                if (result.status() == ProgramResult.Status.ERROR) {
                    finished.logUnexpected();
                }
                yield result;
            }
            case Unfinished unfinished ->
                ProgramResult.failure(
                        unfinished.timedOut() ? ProgramResult.Status.TIMEOUT : ProgramResult.Status.ERROR,
                        unfinished.message());
        };
    }

    /** Lance un conteneur de l'image du langage, avec ces arguments, et lui envoie les fichiers. */
    private Outcome launch(Language language, List<String> arguments, TarContent content) {
        String image = images.get(language);
        if (image == null) {
            LOG.errorf("Aucune image pour le langage %s", language);
            return new Unfinished(false, UNAVAILABLE);
        }
        String name = "recallio-run-" + UUID.randomUUID();
        Process process;
        try {
            process = new ProcessBuilder(command(name, image, arguments)).start();
        } catch (IOException e) {
            LOG.errorf(e, "Impossible de lancer docker");
            return new Unfinished(false, UNAVAILABLE);
        }
        try (ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = readers.submit(() -> read(process.getInputStream()));
            Future<String> stderr = readers.submit(() -> read(process.getErrorStream()));
            send(content, process.getOutputStream());
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                kill(name, process);
                return new Unfinished(true, "L'exécution a dépassé %d secondes.".formatted(timeout.toSeconds()));
            }
            return new Finished(name, process.exitValue(), stdout.get(), stderr.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            kill(name, process);
            return new Unfinished(false, "L'exécution a été interrompue.");
        } catch (ExecutionException e) {
            LOG.errorf(e, "Lecture de la sortie de %s impossible", name);
            return new Unfinished(false, UNAVAILABLE);
        }
    }

    /** La ligne de commande, isolation comprise ; chaque option compte (ADR 0030). */
    List<String> command(String name, String image, List<String> arguments) {
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
        command.addAll(arguments);
        return command;
    }

    /**
     * Lit la sortie d'une soumission. Une sortie qui commence par le marqueur du rapport donne le résultat de chaque
     * test, dans le format du langage (JUnit en XML, {@code test2json} pour Go) ; un rapport sans test veut dire que les
     * tests ne sont pas allés au bout.
     */
    static RunResult interpret(Language language, String stdout) {
        int newline = stdout.indexOf('\n');
        String marker = (newline < 0 ? stdout : stdout.substring(0, newline)).strip();
        String rest = newline < 0 ? "" : stdout.substring(newline + 1);
        if (marker.equals(COMPILATION_ERROR)) {
            return RunResult.failure(Status.COMPILATION_ERROR, truncate(rest.strip()));
        }
        if (!marker.equals(REPORT)) {
            return RunResult.failure(Status.ERROR, UNEXPECTED);
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
                            .formatted(
                                    switch (language) {
                                        case JAVA -> "System.exit";
                                        case GO -> "os.Exit";
                                        case CPP -> "exit, ou un plantage";
                                    }));
        }
        return RunResult.of(tests);
    }

    /**
     * Lit la sortie d'un programme : une ligne de résultat ({@code @@recallio:program <code> <tronqué>}), puis sa sortie
     * standard et sa sortie d'erreur, chacune en base64 sur une ligne.
     */
    static ProgramResult interpretProgram(String stdout, Duration programTimeout) {
        int newline = stdout.indexOf('\n');
        String marker = (newline < 0 ? stdout : stdout.substring(0, newline)).strip();
        String rest = newline < 0 ? "" : stdout.substring(newline + 1);
        if (marker.equals(COMPILATION_ERROR)) {
            return ProgramResult.failure(ProgramResult.Status.COMPILATION_ERROR, truncate(rest.strip()));
        }
        String[] header = marker.split(" ");
        String[] outputs = rest.split("\n", -1);
        if (header.length != 3 || !header[0].equals(PROGRAM) || outputs.length < 2) {
            return ProgramResult.failure(ProgramResult.Status.ERROR, UNEXPECTED);
        }
        try {
            int exitCode = Integer.parseInt(header[1]);
            boolean truncated = header[2].equals("1");
            String out = decode(outputs[0]);
            String err = decode(outputs[1]);
            return exitCode == TIMEOUT_EXIT_CODE
                    ? new ProgramResult(
                            ProgramResult.Status.TIMEOUT,
                            exitCode,
                            out,
                            err,
                            truncated,
                            "Le programme a dépassé %d secondes.".formatted(programTimeout.toSeconds()))
                    : new ProgramResult(ProgramResult.Status.COMPLETED, exitCode, out, err, truncated, "");
        } catch (IllegalArgumentException e) {
            // Un code de sortie qui n'est pas un nombre, ou une sortie qui n'est pas du base64.
            return ProgramResult.failure(ProgramResult.Status.ERROR, UNEXPECTED);
        }
    }

    /** Une sortie coupée au milieu d'un caractère garde ce qui se lit : le reste devient « � ». */
    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64.strip()), StandardCharsets.UTF_8);
    }

    private static List<TestOutcome> report(Language language, String report) {
        return switch (language) {
            case JAVA -> JunitReport.parse(report, JunitReport.Message.ATTRIBUTE);
            case GO -> GoTestReport.parse(report);
            // doctest écrit un rapport au format JUnit ; son texte dit l'assertion et ses valeurs (ADR 0053).
            case CPP -> JunitReport.parse(report, JunitReport.Message.TEXT);
        };
    }

    /** Envoie les fichiers en archive tar ; le conteneur les range dans {@code src/} et {@code tests/}. */
    private static void send(TarContent content, OutputStream stdin) {
        try (var tar = new TarArchiveOutputStream(stdin, StandardCharsets.UTF_8.name())) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            content.writeTo(tar);
        } catch (IOException e) {
            // Le conteneur s'est arrêté avant d'avoir tout lu : sa sortie dira pourquoi.
            LOG.debugf(e, "Envoi des fichiers interrompu");
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

    /** Ce qu'on écrit dans l'archive envoyée au conteneur. */
    @FunctionalInterface
    private interface TarContent {
        void writeTo(TarArchiveOutputStream tar) throws IOException;
    }

    /** Ce qu'a rendu le conteneur. */
    private sealed interface Outcome permits Finished, Unfinished {}

    /** Le conteneur a fini : sa sortie standard, et de quoi comprendre une réponse inattendue. */
    private record Finished(String name, int exitCode, String stdout, String stderr) implements Outcome {

        void logUnexpected() {
            LOG.warnf("Exécution %s en erreur (code %d) : %s", name, exitCode, stderr);
        }
    }

    /** Le conteneur n'a rien rendu : il n'a pas pu être lancé, a dépassé le délai, ou a été interrompu. */
    private record Unfinished(boolean timedOut, String message) implements Outcome {}
}
