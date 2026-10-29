package com.recallio.gamification.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Barème des points et objectif quotidien (CLAUDE.md, section 6.3).
 *
 * <p>Chaque révision rapporte autant de points, quelle que soit la note : récompenser « Facile » pousserait à
 * surévaluer ses réponses, et rien ne récompense la vitesse. Atteindre l'objectif du jour rapporte un bonus, une seule
 * fois par jour.
 *
 * @param dailyGoal nombre de cartes à réviser dans la journée pour qu'elle compte dans le streak
 */
public record ScoringRules(int pointsPerReview, int dailyGoal, int dailyGoalBonus) {

    public ScoringRules {
        if (pointsPerReview < 0 || dailyGoalBonus < 0) {
            throw new IllegalArgumentException("Les points attribués ne peuvent pas être négatifs");
        }
        if (dailyGoal < 1) {
            throw new IllegalArgumentException("L'objectif quotidien doit être d'au moins une carte");
        }
    }

    /**
     * Points gagnés par une révision.
     *
     * @param cardsReviewedToday nombre de cartes révisées aujourd'hui, celle-ci comprise
     */
    public List<PointsAward> awardsFor(int cardsReviewedToday) {
        List<PointsAward> awards = new ArrayList<>();
        if (pointsPerReview > 0) {
            awards.add(new PointsAward(PointsReason.REVIEW, pointsPerReview));
        }
        // Le compteur avance d'une carte à la fois : l'égalité ne se produit qu'une fois par jour.
        if (cardsReviewedToday == dailyGoal && dailyGoalBonus > 0) {
            awards.add(new PointsAward(PointsReason.DAILY_GOAL, dailyGoalBonus));
        }
        return List.copyOf(awards);
    }

    public record PointsAward(PointsReason reason, int amount) {}
}
