package com.recallio.gamification.domain.quest;

/** Ce qui fait avancer une quête (ADR 0040). */
public enum QuestEvent {
    /** Une carte révisée, quels que soient son format et sa note. */
    REVIEW,
    /** Une réussite corrigée par le serveur : un QCM, un vrai ou faux, un code à trous, un exercice. */
    VERIFIED_SUCCESS,
    /** Une carte de code révisée : qu'affiche ce code, bug à corriger, code à compléter, exercice. */
    CODE_CARD,
    /** Un exercice de code réussi, jugé par ses tests. */
    EXERCISE_PASSED,
    /** Un format de carte révisé pour la première fois de la semaine. */
    NEW_FORMAT,
    /** Un jour de la semaine où l'on a révisé, compté une fois. */
    ACTIVE_DAY,
    /** Une page du Tour ouverte pour la première fois de la journée. */
    TOUR_PAGE,
    /** Une notion du Tour validée pour la première fois. */
    TOUR_NOTION
}
