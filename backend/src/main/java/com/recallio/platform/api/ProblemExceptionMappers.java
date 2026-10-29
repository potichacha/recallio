package com.recallio.platform.api;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Traduit les exceptions en réponses Problem Details (RFC 9457) avec un code d'erreur stable. */
public class ProblemExceptionMappers {

    private static final int UNPROCESSABLE_CONTENT = 422;

    @ServerExceptionMapper
    public Response mapDomainException(DomainException exception) {
        record Outcome(int status, String title) {}
        var outcome = switch (exception) {
            case ResourceNotFoundException _ ->
                new Outcome(Response.Status.NOT_FOUND.getStatusCode(), "Ressource introuvable");
            case ConflictException _ -> new Outcome(Response.Status.CONFLICT.getStatusCode(), "Conflit");
            default -> new Outcome(UNPROCESSABLE_CONTENT, "Requête refusée");
        };
        return problem(ProblemDetail.of(outcome.status(), outcome.title(), exception.code(), exception.detail()));
    }

    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException exception) {
        var invalidParams = exception.getConstraintViolations().stream()
                .map(violation -> new ProblemDetail.InvalidParam(
                        lastNode(violation.getPropertyPath().toString()), violation.getMessage()))
                .sorted(Comparator.comparing(ProblemDetail.InvalidParam::name))
                .toList();
        int status = Response.Status.BAD_REQUEST.getStatusCode();
        return problem(new ProblemDetail(
                "about:blank",
                "Requête invalide",
                status,
                "Un ou plusieurs paramètres sont invalides.",
                "validation.failed",
                invalidParams));
    }

    private static Response problem(ProblemDetail body) {
        return Response.status(body.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(body)
                .build();
    }

    /** {@code listCards.size} devient {@code size} : le client ne connaît que le nom du paramètre. */
    private static String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }
}
