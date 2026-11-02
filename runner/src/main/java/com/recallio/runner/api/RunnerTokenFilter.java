package com.recallio.runner.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Seul le backend appelle le runner (ADR 0030) : chaque requête de l'API présente le jeton partagé
 * ({@code Authorization: Bearer <jeton>}), comparé en temps constant. La santé ({@code /q/health}) reste ouverte, pour
 * l'orchestrateur.
 */
public class RunnerTokenFilter {

    /** Un jeton court se devine : au moins 32 caractères, tirés au hasard ({@code openssl rand -hex 32}). */
    static final int MIN_TOKEN_LENGTH = 32;

    private static final String BEARER = "Bearer ";

    private final byte[] expected;

    RunnerTokenFilter(@ConfigProperty(name = "recallio.runner.token") String token) {
        if (token.length() < MIN_TOKEN_LENGTH) {
            throw new IllegalStateException("Le jeton du runner (RECALLIO_RUNNER_TOKEN) doit faire au moins "
                    + MIN_TOKEN_LENGTH + " caractères");
        }
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    @ServerRequestFilter
    public Optional<Response> requireToken(ContainerRequestContext request) {
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        boolean valid = header != null
                && header.startsWith(BEARER)
                && MessageDigest.isEqual(header.substring(BEARER.length()).getBytes(StandardCharsets.UTF_8), expected);
        return valid
                ? Optional.empty()
                : Optional.of(Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
