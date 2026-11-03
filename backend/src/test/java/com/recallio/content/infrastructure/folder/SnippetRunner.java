package com.recallio.content.infrastructure.folder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import org.jspecify.annotations.Nullable;

/**
 * Exécute un extrait de code Java comme JShell (ADR 0028) : des instructions, des expressions et des déclarations sans
 * classe englobante, avec les imports par défaut de JShell ({@code java.util.*}, {@code java.util.stream.*}...).
 *
 * <p>Le code tourne dans une JVM séparée, lancée une fois : une boucle infinie est interrompue au bout de
 * {@link #DEFAULT_TIMEOUT}, et un {@code System.exit} ne touche pas les tests. Les déclarations d'un extrait sont retirées
 * avant le suivant : deux cartes peuvent déclarer chacune leur classe {@code Parent}.
 */
final class SnippetRunner implements AutoCloseable {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Les imports du démarrage par défaut de l'outil {@code jshell} : l'API ne les ajoute pas d'elle-même, et les
     * extraits des cartes les supposent, comme l'écrit le guide de rédaction.
     */
    private static final List<String> DEFAULT_IMPORTS = List.of(
            "java.io",
            "java.math",
            "java.net",
            "java.nio.file",
            "java.util",
            "java.util.concurrent",
            "java.util.function",
            "java.util.prefs",
            "java.util.regex",
            "java.util.stream");

    /**
     * Le résultat d'un extrait : ce qu'il a affiché, puis l'erreur de compilation ou l'exception qui l'a arrêté.
     *
     * @param exception le nom simple de l'exception levée ({@code NullPointerException}), ou {@link #TIMED_OUT}
     */
    record Execution(
            String output,
            @Nullable String compileError,
            @Nullable String exception) {}

    /** L'« exception » d'un extrait interrompu faute d'avoir fini à temps. */
    static final String TIMED_OUT = "délai dépassé";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final JShell shell;
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private final Duration timeout;

    SnippetRunner() {
        this(DEFAULT_TIMEOUT);
    }

    SnippetRunner(Duration timeout) {
        this.timeout = timeout;
        PrintStream console = new PrintStream(output, true, StandardCharsets.UTF_8);
        // La JVM d'exécution écrit en UTF-8 sur tous les systèmes : sinon, sous Windows, un « é » arrive en « ? ».
        shell = JShell.builder()
                .out(console)
                .err(console)
                .remoteVMOptions("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dfile.encoding=UTF-8")
                .build();
        DEFAULT_IMPORTS.forEach(imported -> shell.eval("import " + imported + ".*;"));
    }

    Execution run(String code) {
        output.reset();
        List<Snippet> declared = new ArrayList<>();
        AtomicBoolean timedOut = new AtomicBoolean();
        ScheduledFuture<?> watch = watchdog.schedule(
                () -> {
                    timedOut.set(true);
                    shell.stop();
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS);
        try {
            String remaining = code;
            while (!remaining.isBlank()) {
                CompletionInfo info = shell.sourceCodeAnalysis().analyzeCompletion(remaining);
                boolean complete = info.completeness().isComplete();
                for (SnippetEvent event : shell.eval(complete ? info.source() : remaining)) {
                    declared.add(event.snippet());
                    // JShell ne signale pas un arrêt par stop() : l'événement d'un extrait interrompu semble réussi.
                    Execution stopped = timedOut.get() ? new Execution(printed(), null, TIMED_OUT) : stopped(event);
                    if (stopped != null) {
                        return stopped;
                    }
                }
                remaining = complete ? info.remaining() : "";
            }
            return new Execution(printed(), null, null);
        } finally {
            watch.cancel(false);
            declared.stream()
                    .filter(snippet -> shell.status(snippet).isActive())
                    .forEach(shell::drop);
        }
    }

    /** Le résultat, si cet événement arrête l'extrait : une erreur de compilation ou une exception. */
    private @Nullable Execution stopped(SnippetEvent event) {
        if (event.causeSnippet() != null) {
            return null;
        }
        if (event.status() == Snippet.Status.REJECTED) {
            return new Execution(printed(), diagnostics(event.snippet()), null);
        }
        if (!event.status().isDefined() && event.status().isActive()) {
            return new Execution(
                    printed(), "références non résolues : " + event.snippet().source(), null);
        }
        JShellException exception = event.exception();
        if (exception == null) {
            return null;
        }
        if (exception instanceof EvalException thrown) {
            return new Execution(printed(), null, simpleName(thrown.getExceptionClassName()));
        }
        return new Execution(
                printed(), "références non résolues : " + event.snippet().source(), null);
    }

    private String diagnostics(Snippet snippet) {
        return shell.diagnostics(snippet)
                .map(diagnostic -> diagnostic.getMessage(Locale.ROOT))
                .collect(Collectors.joining(" ; "));
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    @Override
    public void close() {
        watchdog.shutdownNow();
        shell.close();
    }
}
