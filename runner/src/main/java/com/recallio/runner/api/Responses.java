package com.recallio.runner.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Les réponses d'erreur communes aux exécutions de soumissions et de programmes. */
final class Responses {

    private Responses() {}

    /** Une requête bien formée, mais refusée par les règles du bac à sable : le message dit laquelle. */
    static Response badRequest(IllegalArgumentException problem) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(problem.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    /** Toutes les places sont prises : le backend peut réessayer un peu plus tard. */
    static Response busy() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
