package com.recallio.gamification.domain;

/** Pourquoi des points ont été attribués. Stocké tel quel en base : ne pas renommer une valeur existante. */
public enum PointsReason {
    /** Une carte révisée. */
    REVIEW,
    /** L'objectif quotidien vient d'être atteint. */
    DAILY_GOAL
}
