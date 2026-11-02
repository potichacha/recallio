package com.recallio.live.domain;

/** Où en est une partie live (ADR 0022). */
public enum LiveGameStatus {
    /** Salle d'attente : les joueurs arrivent, l'hôte n'a pas encore démarré. */
    LOBBY,
    /** Une question est posée à tous ; le chrono tourne. */
    QUESTION,
    /** La correction de la question est affichée, avant la suivante. */
    REVEAL,
    /** Toutes les questions sont passées. */
    FINISHED
}
