package com.recallio.review.infrastructure.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.exercise.ExerciseRun.Status;
import com.recallio.review.domain.exercise.ExerciseRun.TestResult;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.infrastructure.runner.RunnerClient.FileDto;
import com.recallio.review.infrastructure.runner.RunnerClient.RunRequest;
import com.recallio.review.infrastructure.runner.RunnerClient.RunResponse;
import com.recallio.review.infrastructure.runner.RunnerClient.TestDto;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpCodeRunnerTest {

    private static final List<SourceFile> CODE = List.of(new SourceFile("Doublons.java", "class Doublons {}"));
    private static final List<SourceFile> TESTS = List.of(new SourceFile("DoublonsTest.java", "class DoublonsTest {}"));

    @Test
    void shouldSendTheCodeAndTheTestsAndReadEachResult() {
        var sent = new AtomicReference<RunRequest>();
        var runner = new HttpCodeRunner(request -> {
            sent.set(request);
            return new RunResponse("FAILED", List.of(new TestDto("DoublonsTest", "retire", false, "attendu 2")), "");
        });

        ExerciseRun run = runner.run(CODE, TESTS);

        assertThat(sent.get())
                .isEqualTo(new RunRequest(
                        List.of(new FileDto("Doublons.java", "class Doublons {}")),
                        List.of(new FileDto("DoublonsTest.java", "class DoublonsTest {}"))));
        assertThat(run)
                .isEqualTo(new ExerciseRun(
                        Status.FAILED, List.of(new TestResult("DoublonsTest", "retire", false, "attendu 2")), ""));
    }

    /** Une panne du runner n'est jamais la faute du joueur : sa réponse n'est pas jugée. */
    @Test
    void shouldTreatEveryFailureOfTheRunnerAsUnavailable() {
        assertThatThrownBy(() -> new HttpCodeRunner(_ -> new RunResponse("ERROR", List.of(), "panne")).run(CODE, TESTS))
                .isInstanceOf(RunnerUnavailableException.class);
        assertThatThrownBy(() -> new HttpCodeRunner(_ -> {
                            throw new WebApplicationException(
                                    Response.status(503).build());
                        })
                        .run(CODE, TESTS))
                .isInstanceOf(RunnerUnavailableException.class);
        assertThatThrownBy(() -> new HttpCodeRunner(_ -> {
                            throw new ProcessingException("connexion refusée");
                        })
                        .run(CODE, TESTS))
                .isInstanceOf(RunnerUnavailableException.class);
    }
}
