package com.recallio.gamification.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Activité quotidienne : nombre de cartes révisées par jour local, base du streak. */
public interface ActivityRepository {

    /**
     * Compte une carte révisée de plus ce jour-là, de façon atomique (deux révisions simultanées comptent deux fois).
     *
     * @param goal objectif quotidien en vigueur, pour marquer la journée comme réussie
     * @return le nombre de cartes révisées ce jour-là, celle-ci comprise
     */
    int recordReview(UUID userId, LocalDate day, int goal);

    /** Nombre de cartes révisées ce jour-là (0 si aucune). */
    int cardsReviewed(UUID userId, LocalDate day);

    /** Jours où l'objectif a été atteint (au plus un par jour : quelques centaines de lignes par an). */
    List<LocalDate> goalDays(UUID userId);
}
