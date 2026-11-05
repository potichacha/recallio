package com.recallio.gamification.domain.achievement;

/**
 * Les succès (ADR 0040), débloqués une seule fois, et seulement pour de la régularité ou de vraies réussites : jamais
 * pour la vitesse. Stockés par leur nom : ne pas renommer une valeur existante.
 */
public enum Achievement {
    /** La première carte révisée. */
    FIRST_REVIEW,
    /** 100 cartes révisées. */
    HUNDRED_REVIEWS,
    /** 1 000 cartes révisées. */
    THOUSAND_REVIEWS,
    /** Une série de 7 jours. */
    STREAK_7,
    /** Une série de 30 jours. */
    STREAK_30,
    /** Un premier exercice de code réussi, jugé par ses tests. */
    FIRST_EXERCISE,
    /** Des cartes révisées dans trois sujets. */
    POLYGLOT,
    /** Une première notion du Tour validée. */
    FIRST_TOUR_NOTION,
    /** Dix notions du Tour validées. */
    TOUR_EXPLORER,
    /** Une première quête terminée. */
    FIRST_QUEST,
    /** Une quête de la semaine terminée. */
    WEEKLY_QUEST
}
