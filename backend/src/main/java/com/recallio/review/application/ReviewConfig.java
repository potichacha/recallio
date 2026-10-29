package com.recallio.review.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Réglages de la révision ({@code recallio.review.*}). */
@ConfigMapping(prefix = "recallio.review")
public interface ReviewConfig {

    /** Nombre maximal de cartes vues pour la première fois par jour (dans le fuseau de l'utilisateur). */
    @WithDefault("20")
    int newCardsPerDay();

    /** Probabilité de se souvenir visée par FSRS au moment de chaque révision. */
    @WithDefault("0.9")
    double desiredRetention();
}
