package com.recallio.runner.api;

import com.recallio.runner.api.RunDtos.RunRequest;
import com.recallio.runner.api.RunDtos.RunResultDto;
import com.recallio.runner.sandbox.DockerSandbox;
import com.recallio.runner.sandbox.SandboxConfig;
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
import java.util.concurrent.Semaphore;

/**
 * Exécute une soumission et renvoie le résultat de chaque test (ADR 0030). Synchrone : une exécution dure quelques
 * secondes, bornées par le délai du bac à sable. Réservé au backend, qui présente le jeton du runner
 * ({@link RunnerTokenFilter}).
 */
@Path("/api/v1/runs")
@Singleton
public class RunsResource {

    private final DockerSandbox sandbox;
    private final Semaphore slots;

    RunsResource(DockerSandbox sandbox, SandboxConfig config) {
        this.sandbox = sandbox;
        this.slots = new Semaphore(config.maxConcurrentRuns());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response run(@NotNull @Valid RunRequest request) {
        if (!slots.tryAcquire()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        try {
            return Response.ok(RunResultDto.from(sandbox.run(request.toSubmission())))
                    .build();
        } finally {
            slots.release();
        }
    }
}
