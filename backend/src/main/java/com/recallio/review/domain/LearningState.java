package com.recallio.review.domain;

/** Étape de mémorisation d'une carte pour un utilisateur. Stockée telle quelle en base : ne pas renommer. */
public enum LearningState {
    /** Jamais révisée : aucune ligne en base. */
    NEW,
    /** Premières révisions, à intervalles courts (minutes). */
    LEARNING,
    /** Mémorisée : intervalles en jours, calculés par FSRS. */
    REVIEW,
    /** Oubliée après avoir été mémorisée : de nouveau des intervalles courts. */
    RELEARNING
}
