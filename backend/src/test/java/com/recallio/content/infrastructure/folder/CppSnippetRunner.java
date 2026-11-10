package com.recallio.content.infrastructure.folder;

import com.recallio.content.infrastructure.folder.NativeProcess.Result;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Exécute un programme C++ complet (ses {@code #include} et sa fonction {@code main}) avec le {@code g++} de la machine
 * (ADR 0053), comme {@link GoSnippetRunner} le fait pour Go : compilation, puis exécution bornée par un délai.
 *
 * <p>Le compilateur est celui du bac à sable, GCC 14, avec les mêmes options, lues dans {@code sandbox/cpp/flags} : un
 * autre compilateur pourrait afficher autre chose.
 */
final class CppSnippetRunner {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(1);

    /** La version de GCC du bac à sable ({@code sandbox/cpp/Dockerfile}). */
    static final String GCC_MAJOR = "14";

    /** Les options du bac à sable : un seul fichier les fixe, pour l'image comme pour la CI. */
    private static final Path FLAGS = Path.of("..", "sandbox", "cpp", "flags");

    /** Les compilateurs essayés : celui de la CI, puis celui du système s'il est en version 14. */
    private static final List<String> COMPILERS = List.of("g++-" + GCC_MAJOR, "g++");

    /** Ce qu'écrit la bibliothèque standard quand une exception n'est rattrapée par personne. */
    private static final Pattern UNCAUGHT = Pattern.compile("terminate called after throwing an instance of '([^']+)'");

    /** Le « motif d'arrêt » d'un programme interrompu faute d'avoir fini à temps. */
    static final String TIMED_OUT = "délai dépassé";

    /**
     * Le résultat d'un programme : ce qu'il a affiché, puis l'erreur de compilation ou ce qui l'a arrêté.
     *
     * @param stop le type de l'exception non rattrapée ({@code std::out_of_range}), son code de sortie s'il a échoué
     *     autrement, ou {@link #TIMED_OUT}
     */
    record Execution(
            String output,
            @Nullable String compileError,
            @Nullable String stop) {}

    private final String compiler;
    private final List<String> flags;

    private CppSnippetRunner(String compiler, List<String> flags) {
        this.compiler = compiler;
        this.flags = List.copyOf(flags);
    }

    /**
     * L'exécuteur, si un {@code g++} en version 14 est installé, avec {@code stdbuf} : le programme écrit sa sortie
     * ligne par ligne, comme dans un terminal et dans le bac à sable. Sinon, ce qu'il affiche avant une exception non
     * rattrapée resterait dans son tampon, perdu.
     */
    static Optional<CppSnippetRunner> find() {
        if (!succeeds(List.of("stdbuf", "--version"))) {
            return Optional.empty();
        }
        for (String compiler : COMPILERS) {
            try {
                Result version = NativeProcess.execute(Path.of("."), List.of(compiler, "-dumpversion"), TIMEOUT);
                if (!version.timedOut()
                        && version.exitCode() == 0
                        && version.stdout().strip().split("\\.")[0].equals(GCC_MAJOR)) {
                    return Optional.of(new CppSnippetRunner(compiler, flags()));
                }
            } catch (IOException e) {
                // Ce compilateur n'est pas installé : on essaie le suivant.
            }
        }
        return Optional.empty();
    }

    private static boolean succeeds(List<String> command) {
        try {
            Result result = NativeProcess.execute(Path.of("."), command, TIMEOUT);
            return !result.timedOut() && result.exitCode() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static List<String> flags() {
        try {
            return Arrays.asList(Files.readString(FLAGS).strip().split("\\s+"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Execution run(String program) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("recallio-cpp-");
            Files.writeString(directory.resolve("main.cpp"), program);
            Path binary = directory.resolve(NativeProcess.executable("extrait"));
            List<String> command = new ArrayList<>(List.of(compiler));
            command.addAll(flags);
            command.addAll(List.of("-o", binary.toString(), "main.cpp"));
            Result build = NativeProcess.execute(directory, command, BUILD_TIMEOUT);
            if (build.timedOut()) {
                throw new IllegalStateException("g++ n'a pas fini en " + BUILD_TIMEOUT);
            }
            if (build.exitCode() != 0) {
                return new Execution("", build.stderr().isBlank() ? build.stdout() : build.stderr(), null);
            }
            Result run = NativeProcess.execute(directory, List.of("stdbuf", "-oL", binary.toString()), TIMEOUT);
            if (run.timedOut()) {
                return new Execution(run.stdout(), null, TIMED_OUT);
            }
            return new Execution(run.stdout(), null, run.exitCode() == 0 ? null : stop(run));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            NativeProcess.delete(directory);
        }
    }

    /** Ce qui a arrêté le programme : le type de l'exception non rattrapée, ou son code de sortie. */
    private static String stop(Result run) {
        Matcher uncaught = UNCAUGHT.matcher(run.stderr());
        return uncaught.find() ? uncaught.group(1) : "code de sortie " + run.exitCode();
    }
}
