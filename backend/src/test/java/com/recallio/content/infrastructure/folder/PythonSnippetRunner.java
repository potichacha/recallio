package com.recallio.content.infrastructure.folder;

import com.recallio.content.infrastructure.folder.NativeProcess.Result;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Exécute un programme Python complet avec le {@code python} 3.14 de la machine (ADR 0062), comme
 * {@link CppSnippetRunner} le fait pour C++ : la syntaxe d'abord, qui tient lieu de compilation, puis l'exécution,
 * bornée par un délai.
 *
 * <p>La version est celle du bac à sable : les messages d'erreur de Python changent d'une version à l'autre. Les
 * options sont celles de l'image : UTF-8 quelle que soit la locale ({@code -X utf8}), sorties sans tampon ({@code -u}),
 * et pas de {@code .pyc} écrits ({@code -B}).
 */
final class PythonSnippetRunner {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** La version de Python du bac à sable ({@code sandbox/python/Dockerfile}). */
    static final String PYTHON_VERSION = "3.14";

    /** Les interpréteurs essayés : celui de la version du bac à sable, puis celui du système s'il est en 3.14. */
    private static final List<String> INTERPRETERS = List.of("python" + PYTHON_VERSION, "python3", "python");

    private static final List<String> OPTIONS = List.of("-X", "utf8", "-u", "-B");

    /** Vérifie la syntaxe du programme sans l'exécuter, comme {@code run.sh} dans le bac à sable. */
    private static final String CHECK_SYNTAX =
            "import sys; compile(open(sys.argv[1], encoding='utf-8').read(), sys.argv[1], 'exec')";

    /** La dernière ligne d'une pile Python : le type de l'exception ({@code KeyError}), puis son message éventuel. */
    private static final Pattern UNCAUGHT = Pattern.compile("([A-Za-z_][\\w.]*)(?::.*)?");

    /** Le « motif d'arrêt » d'un programme interrompu faute d'avoir fini à temps. */
    static final String TIMED_OUT = "délai dépassé";

    /**
     * Le résultat d'un programme : ce qu'il a affiché, puis l'erreur de syntaxe ou ce qui l'a arrêté.
     *
     * @param stop le type de l'exception non rattrapée ({@code KeyError}), son code de sortie s'il a échoué autrement,
     *     ou {@link #TIMED_OUT}
     */
    record Execution(
            String output,
            @Nullable String compileError,
            @Nullable String stop) {}

    private final String interpreter;

    private PythonSnippetRunner(String interpreter) {
        this.interpreter = interpreter;
    }

    /** L'exécuteur, si un Python en version 3.14 est installé. */
    static Optional<PythonSnippetRunner> find() {
        for (String interpreter : INTERPRETERS) {
            try {
                Result version = NativeProcess.execute(
                        Path.of("."),
                        List.of(interpreter, "-c", "import sys; print('%d.%d' % sys.version_info[:2])"),
                        TIMEOUT);
                if (!version.timedOut()
                        && version.exitCode() == 0
                        && version.stdout().strip().equals(PYTHON_VERSION)) {
                    return Optional.of(new PythonSnippetRunner(interpreter));
                }
            } catch (IOException e) {
                // Cet interpréteur n'est pas installé : on essaie le suivant.
            }
        }
        return Optional.empty();
    }

    Execution run(String program) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("recallio-python-");
            Files.writeString(directory.resolve("main.py"), program, StandardCharsets.UTF_8);
            Result syntax = NativeProcess.execute(directory, command("-c", CHECK_SYNTAX, "main.py"), TIMEOUT);
            if (syntax.timedOut()) {
                throw new IllegalStateException("python n'a pas vérifié la syntaxe en " + TIMEOUT);
            }
            if (syntax.exitCode() != 0) {
                return new Execution("", syntax.stderr(), null);
            }
            Result run = NativeProcess.execute(directory, command("main.py"), TIMEOUT);
            // Sous Windows, print écrit ses fins de ligne en \r\n : la sortie se compare comme sous Linux.
            String output = run.stdout().replace("\r\n", "\n");
            if (run.timedOut()) {
                return new Execution(output, null, TIMED_OUT);
            }
            return new Execution(output, null, run.exitCode() == 0 ? null : stop(run));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            NativeProcess.delete(directory);
        }
    }

    private List<String> command(String... arguments) {
        List<String> command = new ArrayList<>(List.of(interpreter));
        command.addAll(OPTIONS);
        command.addAll(List.of(arguments));
        return command;
    }

    /** Ce qui a arrêté le programme : le type de l'exception non rattrapée, ou son code de sortie. */
    private static String stop(Result run) {
        String stderr = run.stderr().strip();
        if (stderr.contains("Traceback (most recent call last):")) {
            Matcher uncaught = UNCAUGHT.matcher(stderr.substring(stderr.lastIndexOf('\n') + 1));
            if (uncaught.matches()) {
                return uncaught.group(1);
            }
        }
        return "code de sortie " + run.exitCode();
    }
}
