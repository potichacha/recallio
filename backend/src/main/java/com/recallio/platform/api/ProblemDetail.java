package com.recallio.platform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corps d'erreur au format RFC 9457 (Problem Details). {@code code} est un identifiant stable, utilisable par le client
 * pour adapter son message ; {@code detail} est destiné aux humains et peut évoluer.
 */
@Schema(
        name = "ProblemDetail",
        requiredProperties = {"type", "title", "status", "detail", "code"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemDetail(
        String type, String title, int status, String detail, String code, List<InvalidParam> invalidParams) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public ProblemDetail {
        invalidParams = List.copyOf(invalidParams);
    }

    public static ProblemDetail of(int status, String title, String code, String detail) {
        return new ProblemDetail("about:blank", title, status, detail, code, List.of());
    }

    /** Un paramètre de requête invalide (extension courante de la RFC 9457). */
    public record InvalidParam(String name, String reason) {}
}
