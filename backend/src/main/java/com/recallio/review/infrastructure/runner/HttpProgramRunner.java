package com.recallio.review.infrastructure.runner;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramRunner;
import com.recallio.review.domain.playground.ProgramStatus;
import com.recallio.review.infrastructure.runner.ProgramsClient.ProgramRequest;
import com.recallio.review.infrastructure.runner.ProgramsClient.ProgramResponse;
import com.recallio.review.infrastructure.runner.RunnerClient.FileDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Le port {@link ProgramRunner}, implémenté par un appel HTTP au mode « programme » du runner (ADR 0037). Comme pour les
 * exercices ({@link HttpCodeRunner}), une panne du runner devient {@link RunnerUnavailableException}.
 */
@ApplicationScoped
public class HttpProgramRunner implements ProgramRunner {

    private static final Logger LOG = Logger.getLogger(HttpProgramRunner.class);

    private final ProgramsClient client;

    HttpProgramRunner(@RestClient ProgramsClient client) {
        this.client = client;
    }

    @Override
    public ProgramRun run(CodeLanguage language, List<SourceFile> files) {
        ProgramResponse response;
        try {
            response = client.run(new ProgramRequest(
                    language.name(),
                    files.stream()
                            .map(file -> new FileDto(file.path(), file.content()))
                            .toList()));
        } catch (WebApplicationException e) {
            LOG.warnf("Le runner a répondu %d", e.getResponse().getStatus());
            throw new RunnerUnavailableException();
        } catch (ProcessingException e) {
            LOG.warnf(e, "Le runner est injoignable");
            throw new RunnerUnavailableException();
        }
        return new ProgramRun(
                status(response.status()),
                response.exitCode(),
                response.stdout(),
                response.stderr(),
                response.truncated(),
                response.message());
    }

    /** Un statut que le backend ne connaît pas, ou {@code ERROR}, est une panne du runner, pas la faute de l'utilisateur. */
    private static ProgramStatus status(String status) {
        try {
            return ProgramStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Le runner n'a pas pu lancer le programme (statut %s)", status);
            throw new RunnerUnavailableException();
        }
    }
}
