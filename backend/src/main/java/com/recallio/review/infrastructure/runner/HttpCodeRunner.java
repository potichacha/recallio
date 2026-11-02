package com.recallio.review.infrastructure.runner;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.CodeRunner;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.exercise.ExerciseRun.Status;
import com.recallio.review.domain.exercise.ExerciseRun.TestResult;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.infrastructure.runner.RunnerClient.FileDto;
import com.recallio.review.infrastructure.runner.RunnerClient.RunRequest;
import com.recallio.review.infrastructure.runner.RunnerClient.RunResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Le port {@link CodeRunner}, implémenté par un appel HTTP au service runner (ADR 0030). Une panne du runner (injoignable,
 * saturé, en erreur) devient {@link RunnerUnavailableException} : la réponse du joueur n'est pas jugée.
 */
@ApplicationScoped
public class HttpCodeRunner implements CodeRunner {

    private static final Logger LOG = Logger.getLogger(HttpCodeRunner.class);

    private final RunnerClient client;

    HttpCodeRunner(@RestClient RunnerClient client) {
        this.client = client;
    }

    @Override
    public ExerciseRun run(List<SourceFile> code, List<SourceFile> tests) {
        RunResponse response;
        try {
            response = client.run(new RunRequest(files(code), files(tests)));
        } catch (WebApplicationException e) {
            LOG.warnf("Le runner a répondu %d", e.getResponse().getStatus());
            throw new RunnerUnavailableException();
        } catch (ProcessingException e) {
            LOG.warnf(e, "Le runner est injoignable");
            throw new RunnerUnavailableException();
        }
        Status status = status(response.status());
        return new ExerciseRun(
                status,
                response.tests().stream()
                        .map(test -> new TestResult(test.className(), test.name(), test.passed(), test.message()))
                        .toList(),
                response.output());
    }

    /** Un statut que le backend ne connaît pas, ou {@code ERROR}, est une panne du runner, pas un échec du joueur. */
    private static Status status(String status) {
        try {
            return Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Le runner n'a pas pu exécuter le code (statut %s)", status);
            throw new RunnerUnavailableException();
        }
    }

    private static List<FileDto> files(List<SourceFile> files) {
        return files.stream()
                .map(file -> new FileDto(file.path(), file.content()))
                .toList();
    }
}
