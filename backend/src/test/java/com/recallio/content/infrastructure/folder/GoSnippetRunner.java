package com.recallio.content.infrastructure.folder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Exécute un programme Go complet ({@code package main} et {@code func main}) avec la chaîne d'outils {@code go} de la
 * machine (ADR 0033), comme {@link SnippetRunner} le fait pour Java avec JShell : compilation, puis exécution bornée par
 * un délai.
 *
 * <p>Le contenu est relu par pull request : il n'a pas besoin du bac à sable des joueurs. Le module est écrit pour la
 * version de Go installée, et la compilation profite du cache de {@code go} entre deux cartes.
 */
final class GoSnippetRunner {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /** La compilation a son propre délai : la première, à cache froid, recompile la bibliothèque standard. */
    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(3);

    /**
     * Le résultat d'un programme : ce qu'il a affiché, puis l'erreur de compilation ou le message qui l'a arrêté.
     *
     * @param panic la première ligne du message d'arrêt ({@code panic: ...}, {@code fatal error: ...}), ou
     *     {@link #TIMED_OUT}
     */
    record Execution(
            String output,
            @Nullable String compileError,
            @Nullable String panic) {}

    /** Le « message d'arrêt » d'un programme interrompu faute d'avoir fini à temps. */
    static final String TIMED_OUT = "délai dépassé";

    private final String goVersion;
    private final Duration timeout;

    private GoSnippetRunner(String goVersion, Duration timeout) {
        this.goVersion = goVersion;
        this.timeout = timeout;
    }

    /** L'exécuteur, si {@code go} est installé ; sa version de langage ({@code 1.27}) va dans chaque {@code go.mod}. */
    static Optional<GoSnippetRunner> find() {
        try {
            Process process = new ProcessBuilder("go", "env", "GOVERSION")
                    .redirectErrorStream(true)
                    .start();
            String version = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (!process.waitFor(DEFAULT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    || process.exitValue() != 0
                    || !version.startsWith("go")) {
                return Optional.empty();
            }
            // go1.27.1 → 1.27 : la version de langage d'un go.mod.
            String[] parts = version.substring(2).split("\\.");
            return Optional.of(new GoSnippetRunner(parts[0] + "." + parts[1], DEFAULT_TIMEOUT));
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    Execution run(String program) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("recallio-go-");
            Files.writeString(directory.resolve("go.mod"), "module extrait\n\ngo " + goVersion + "\n");
            Files.writeString(directory.resolve("main.go"), program);
            Path binary = directory.resolve(isWindows() ? "extrait.exe" : "extrait");
            Result build = execute(directory, List.of("go", "build", "-o", binary.toString(), "."), BUILD_TIMEOUT);
            if (build.timedOut()) {
                throw new IllegalStateException("go build n'a pas fini en " + BUILD_TIMEOUT);
            }
            if (build.exitCode() != 0) {
                return new Execution("", build.stderr().isBlank() ? build.stdout() : build.stderr(), null);
            }
            Result run = execute(directory, List.of(binary.toString()), timeout);
            if (run.timedOut()) {
                return new Execution(run.stdout(), null, TIMED_OUT);
            }
            return new Execution(run.stdout(), null, run.exitCode() == 0 ? null : stopMessage(run.stderr()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            delete(directory);
        }
    }

    /** La première ligne qui dit pourquoi le programme s'est arrêté, sans la pile d'appels qui suit. */
    private static String stopMessage(String stderr) {
        return stderr.lines()
                .filter(line -> line.startsWith("panic: ") || line.startsWith("fatal error: "))
                .findFirst()
                .orElse(stderr.strip());
    }

    private record Result(int exitCode, String stdout, String stderr, boolean timedOut) {}

    private static Result execute(Path directory, List<String> command, Duration timeout) throws IOException {
        Process process =
                new ProcessBuilder(command).directory(directory.toFile()).start();
        CompletableFuture<String> stdout = read(process.getInputStream());
        CompletableFuture<String> stderr = read(process.getErrorStream());
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor();
                return new Result(-1, stdout.get(), stderr.get(), true);
            }
            return new Result(process.exitValue(), stdout.get(), stderr.get(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new UncheckedIOException(new IOException(e.getCause()));
        }
    }

    private static CompletableFuture<String> read(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    private static void delete(@Nullable Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            // Un dossier temporaire oublié ne fausse aucun résultat.
        }
    }
}
