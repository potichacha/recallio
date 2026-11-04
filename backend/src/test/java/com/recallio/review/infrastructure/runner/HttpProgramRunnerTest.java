package com.recallio.review.infrastructure.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramStatus;
import com.recallio.review.infrastructure.runner.ProgramsClient.ProgramRequest;
import com.recallio.review.infrastructure.runner.ProgramsClient.ProgramResponse;
import com.recallio.review.infrastructure.runner.RunnerClient.FileDto;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpProgramRunnerTest {

    private static final List<SourceFile> MAIN = List.of(new SourceFile("main.go", "package main\n"));

    @Test
    void shouldSendTheFilesAndReadWhatTheProgramWrote() {
        var sent = new AtomicReference<ProgramRequest>();
        var runner = new HttpProgramRunner(request -> {
            sent.set(request);
            return new ProgramResponse("TIMEOUT", 124, "début\n", "", true, "Le programme a dépassé 10 secondes.");
        });

        ProgramRun run = runner.run(CodeLanguage.GO, MAIN);

        assertThat(sent.get()).isEqualTo(new ProgramRequest("GO", List.of(new FileDto("main.go", "package main\n"))));
        assertThat(run)
                .isEqualTo(new ProgramRun(
                        ProgramStatus.TIMEOUT, 124, "début\n", "", true, "Le programme a dépassé 10 secondes."));
    }

    /** Une panne du runner n'est jamais la faute de l'utilisateur. */
    @Test
    void shouldTreatEveryFailureOfTheRunnerAsUnavailable() {
        assertThatThrownBy(() -> new HttpProgramRunner(_ -> new ProgramResponse("ERROR", 0, "", "", false, "panne"))
                        .run(CodeLanguage.GO, MAIN))
                .isInstanceOf(RunnerUnavailableException.class);
        assertThatThrownBy(() -> new HttpProgramRunner(_ -> {
                            throw new WebApplicationException(
                                    Response.status(503).build());
                        })
                        .run(CodeLanguage.GO, MAIN))
                .isInstanceOf(RunnerUnavailableException.class);
        assertThatThrownBy(() -> new HttpProgramRunner(_ -> {
                            throw new ProcessingException("injoignable");
                        })
                        .run(CodeLanguage.GO, MAIN))
                .isInstanceOf(RunnerUnavailableException.class);
    }
}
