package com.recallio.review.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Réglages de la révision ({@code recallio.review.*}). */
@ConfigMapping(prefix = "recallio.review")
public interface ReviewConfig {

    /**
     * Nombre maximal de cartes vues pour la première fois par jour (dans le fuseau de l'utilisateur), dans chaque sujet
     * (ADR 0048).
     */
    @WithDefault("20")
    int newCardsPerSubjectPerDay();

    /** Probabilité de se souvenir visée par FSRS au moment de chaque révision. */
    @WithDefault("0.9")
    double desiredRetention();

    /**
     * Nombre de réponses qu'un compte sans Plus fait corriger par jour (dans son fuseau) en révision intensive
     * (ADR 0079).
     */
    @WithDefault("20")
    int drillFreeAnswersPerDay();
}
