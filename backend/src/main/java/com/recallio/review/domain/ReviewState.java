package com.recallio.review.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Mémoire d'une carte pour un utilisateur (CLAUDE.md, section 6.2).
 *
 * @param step étape d'apprentissage en cours, significative seulement en {@code LEARNING} et {@code RELEARNING}
 * @param stability FSRS : nombre de jours au bout duquel la probabilité de se souvenir tombe à 90 % (0 si nouvelle)
 * @param difficulty FSRS : entre 1 (facile) et 10 (difficile) (0 si nouvelle)
 * @param lapses nombre d'oublis d'une carte mémorisée
 * @param lastReviewAt {@code null} seulement pour une carte nouvelle
 */
public record ReviewState(
        LearningState state,
        int step,
        double stability,
        double difficulty,
        Instant dueAt,
        int reps,
        int lapses,
        @Nullable Instant lastReviewAt) {

    public static ReviewState newCard(Instant now) {
        return new ReviewState(LearningState.NEW, 0, 0, 0, now, 0, 0, null);
    }

    public boolean isDue(Instant now) {
        return !dueAt.isAfter(now);
    }
}
