package com.recallio.runner.api;

import com.recallio.runner.api.RunDtos.ProgramRequest;
import com.recallio.runner.api.RunDtos.ProgramResultDto;
import com.recallio.runner.sandbox.DockerSandbox;
import com.recallio.runner.sandbox.Program;
import com.recallio.runner.sandbox.RunSlots;
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
 * Lance un programme et renvoie ce qu'il a écrit (ADR 0037) : l'exemple d'une page du Tour, ou ce qu'en a fait
 * l'utilisateur. Synchrone et réservé au backend, comme les soumissions ({@link RunsResource}), avec qui il partage les
 * places.
 */
@Path("/api/v1/programs")
@Singleton
public class ProgramsResource {

    private final DockerSandbox sandbox;
    private final RunSlots slots;

    ProgramsResource(DockerSandbox sandbox, RunSlots slots) {
        this.sandbox = sandbox;
        this.slots = slots;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response run(@NotNull @Valid ProgramRequest request) {
        Program program;
        try {
            program = request.toProgram();
        } catch (IllegalArgumentException e) {
            // Des fichiers qui ne sont pas ceux du langage annoncé, ou un programme Java sans Main.java.
            return Responses.badRequest(e);
        }
        return slots.tryRun(() ->
                        Response.ok(ProgramResultDto.from(sandbox.run(program))).build())
                .orElseGet(Responses::busy);
    }
}
