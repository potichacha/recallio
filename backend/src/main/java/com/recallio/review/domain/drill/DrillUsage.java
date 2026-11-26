package com.recallio.review.domain.drill;

import java.time.LocalDate;

/**
 * L'usage de la révision intensive par un compte, un jour donné (ADR 0078) : la seule chose qu'elle garde.
 *
 * @param day le jour de l'utilisateur, dans son fuseau
 * @param answers les réponses que le serveur a corrigées ce jour-là
 * @param correct celles qui étaient justes
 */
public record DrillUsage(LocalDate day, int answers, int correct) {

    /** Un jour sans aucune réponse. */
    public static DrillUsage none(LocalDate day) {
        return new DrillUsage(day, 0, 0);
    }
}
