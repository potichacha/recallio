package com.recallio.runner.api;

import com.recallio.runner.api.RunDtos.RunRequest;
import com.recallio.runner.api.RunDtos.RunResultDto;
import com.recallio.runner.sandbox.DockerSandbox;
import com.recallio.runner.sandbox.RunSlots;
import com.recallio.runner.sandbox.Submission;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exécute une soumission et renvoie le résultat de chaque test (ADR 0030). Synchrone : une exécution dure quelques
 * secondes, bornées par le délai du bac à sable. Réservé au backend, qui présente le jeton du runner
 * ({@link RunnerTokenFilter}).
 */
@Path("/api/v1/runs")
@Singleton
public class RunsResource {

    private final DockerSandbox sandbox;
    private final RunSlots slots;

    RunsResource(DockerSandbox sandbox, RunSlots slots) {
        this.sandbox = sandbox;
        this.slots = slots;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response run(@NotNull @Valid RunRequest request) {
        Submission submission;
        try {
            submission = request.toSubmission();
        } catch (IllegalArgumentException e) {
            // Des fichiers qui ne sont pas ceux du langage annoncé, ou un test qui remplacerait un fichier du code.
            return Responses.badRequest(e);
        }
        return slots.tryRun(() ->
                        Response.ok(RunResultDto.from(sandbox.run(submission))).build())
                .orElseGet(Responses::busy);
    }
}
