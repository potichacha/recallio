package com.recallio.review.domain.daily;

/** Où en est un joueur de son défi du jour (ADR 0067). */
public enum DailyChallengeState {
    /** Aucune réponse encore. */
    NOT_STARTED,
    /** Une réponse au moins, mais pas à toutes les questions : on peut s'arrêter et reprendre. */
    IN_PROGRESS,
    /** Chaque question a sa réponse : le score est connu, les chiffres du jour se montrent. */
    DONE
}
