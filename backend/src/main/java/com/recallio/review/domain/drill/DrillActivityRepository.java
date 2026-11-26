package com.recallio.review.domain.drill;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Le compteur du jour de la révision intensive (ADR 0078) : une ligne par compte et par jour, jamais une ligne par
 * réponse.
 */
public interface DrillActivityRepository {

    /** Compte une réponse corrigée de plus ce jour-là, et rend le compteur après elle. */
    DrillUsage record(UUID userId, LocalDate day, boolean correct);

    /** Le compteur de ce jour ; à zéro s'il n'y a eu aucune réponse. */
    DrillUsage find(UUID userId, LocalDate day);
}
