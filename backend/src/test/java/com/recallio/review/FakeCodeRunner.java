package com.recallio.review;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.CodeRunner;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.exercise.ExerciseRun.Status;
import com.recallio.review.domain.exercise.ExerciseRun.TestResult;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Le runner des tests du backend : il remplace l'appel HTTP au service runner, qui a ses propres tests avec Docker
 * ({@code runner/}). Chaque test choisit le résultat qu'il renvoie, et vérifie ce qu'on lui a envoyé.
 */
@Mock
@ApplicationScoped
public class FakeCodeRunner implements CodeRunner {

    public static final ExerciseRun PASSED =
            new ExerciseRun(Status.PASSED, List.of(new TestResult("DoublonsTest", "retireLesDoublons", true, "")), "");

    public static final ExerciseRun FAILED = new ExerciseRun(
            Status.FAILED,
            List.of(new TestResult(
                    "DoublonsTest", "retireLesDoublons", false, "expected: <[b, a]> but was: <[b, a, b]>")),
            "");

    private volatile ExerciseRun next = PASSED;
    private volatile boolean unavailable;
    private volatile List<SourceFile> lastCode = List.of();
    private volatile List<SourceFile> lastTests = List.of();

    @Override
    public ExerciseRun run(List<SourceFile> code, List<SourceFile> tests) {
        lastCode = List.copyOf(code);
        lastTests = List.copyOf(tests);
        if (unavailable) {
            throw new RunnerUnavailableException();
        }
        return next;
    }

    public void willReturn(ExerciseRun run) {
        next = run;
        unavailable = false;
    }

    public void willBeUnavailable() {
        unavailable = true;
    }

    public List<SourceFile> lastCode() {
        return lastCode;
    }

    public List<SourceFile> lastTests() {
        return lastTests;
    }
}
