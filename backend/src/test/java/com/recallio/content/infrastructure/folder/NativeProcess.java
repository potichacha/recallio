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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Lance une commande de la machine (un compilateur, le programme compilé) et attend sa fin, bornée par un délai : ce
 * que partagent {@link GoSnippetRunner} et {@link CppSnippetRunner}.
 */
final class NativeProcess {

    private NativeProcess() {}

    /** Ce qu'a donné une commande ; {@code timedOut} si le délai l'a interrompue. */
    record Result(int exitCode, String stdout, String stderr, boolean timedOut) {}

    /** @throws IOException si la commande n'existe pas */
    static Result execute(Path directory, List<String> command, Duration timeout) throws IOException {
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

    /** Le nom d'un exécutable sur cette machine : avec {@code .exe} sous Windows. */
    static String executable(String name) {
        return System.getProperty("os.name", "").startsWith("Windows") ? name + ".exe" : name;
    }

    /** Supprime un dossier temporaire et son contenu ; un dossier oublié ne fausse aucun résultat. */
    static void delete(@Nullable Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            // Rien à faire : le dossier est dans le répertoire temporaire du système.
        }
    }
}
