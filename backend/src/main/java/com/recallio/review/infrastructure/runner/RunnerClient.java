package com.recallio.review.infrastructure.runner;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * L'API du runner (ADR 0030, {@code runner/README.md}), vue du backend. Chaque appel présente le jeton partagé.
 * L'adresse et les délais sont dans {@code quarkus.rest-client.runner.*}.
 */
@RegisterRestClient(configKey = "runner")
@Path("/api/v1/runs")
@ClientHeaderParam(name = "Authorization", value = "Bearer ${recallio.runner.token}")
interface RunnerClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    RunResponse run(RunRequest request);

    /** @param language {@code JAVA}, {@code GO} ou {@code CPP} (ADR 0033, 0053) */
    record RunRequest(String language, List<FileDto> code, List<FileDto> tests) {}

    record FileDto(String path, String content) {}

    /** @param status {@code PASSED}, {@code FAILED}, {@code COMPILATION_ERROR}, {@code TIMEOUT} ou {@code ERROR} */
    record RunResponse(String status, List<TestDto> tests, String output) {}

    record TestDto(String className, String name, boolean passed, String message) {}
}
