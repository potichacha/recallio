package com.recallio.gamification.domain;

/** Pourquoi de l'XP a été attribuée (ADR 0035). Stocké tel quel en base : ne pas renommer une valeur existante. */
public enum XpReason {
    /** Une carte révisée, quelle que soit la note. */
    REVIEW,
    /** Une réussite corrigée par le serveur : un QCM, un vrai ou faux, un code à compléter juste, un exercice résolu. */
    VERIFIED_SUCCESS,
    /** L'objectif quotidien vient d'être atteint. */
    DAILY_GOAL,
    /** Une quête du jour ou de la semaine vient d'être terminée (ADR 0040). */
    QUEST
}
