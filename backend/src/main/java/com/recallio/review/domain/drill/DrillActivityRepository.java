package com.recallio.review.domain.drill;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Le compteur du jour de la révision intensive (ADR 0078) : une ligne par compte et par jour, jamais une ligne par
 * réponse.
 */
public interface DrillActivityRepository {

    /**
     * Compte une réponse corrigée de plus ce jour-là, sauf si elle l'est déjà ou si la limite du jour est atteinte
     * (ADR 0079). Une seule instruction décide : deux réponses simultanées ne dépassent pas la limite.
     *
     * @param checkId l'empreinte de la réponse ({@link DrillCheckId}) : une réponse déjà comptée n'est pas recomptée
     * @param dailyLimit le nombre de réponses que le compte peut faire corriger ce jour-là
     * @return le compteur après elle, qu'elle vienne d'être comptée ou qu'elle le soit déjà ; vide si la limite la
     *     refuse
     */
    Optional<DrillUsage> record(UUID userId, LocalDate day, boolean correct, UUID checkId, int dailyLimit);

    /** Le compteur de ce jour ; à zéro s'il n'y a eu aucune réponse. */
    DrillUsage find(UUID userId, LocalDate day);
}
