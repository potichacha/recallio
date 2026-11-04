package com.recallio.review.infrastructure.runner;

import com.recallio.review.infrastructure.runner.RunnerClient.FileDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Le mode « programme » du runner (ADR 0037, {@code runner/README.md}), vue du backend : même adresse, même jeton et
 * mêmes délais que {@link RunnerClient} ({@code quarkus.rest-client.runner.*}).
 */
@RegisterRestClient(configKey = "runner")
@Path("/api/v1/programs")
@ClientHeaderParam(name = "Authorization", value = "Bearer ${recallio.runner.token}")
interface ProgramsClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ProgramResponse run(ProgramRequest request);

    /** @param language {@code JAVA} ou {@code GO} */
    record ProgramRequest(String language, List<FileDto> files) {}

    /** @param status {@code COMPLETED}, {@code COMPILATION_ERROR}, {@code TIMEOUT} ou {@code ERROR} */
    record ProgramResponse(
            String status, int exitCode, String stdout, String stderr, boolean truncated, String message) {}
}
