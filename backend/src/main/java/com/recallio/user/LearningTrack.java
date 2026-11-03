package com.recallio.user;

/**
 * Le parcours d'un sujet suivi (ADR 0032) : il change l'ordre et le choix des nouvelles cartes, jamais les cartes dues,
 * qui passent toujours en premier. Stocké tel quel en base : ne pas renommer une valeur existante.
 */
public enum LearningTrack {
    /** Apprendre le sujet : seulement les notions dont les prérequis sont acquis, du débutant vers l'expert. */
    LEARN,
    /** Préparer un entretien : les notions par priorité, les plus essentielles d'abord, sans contrainte de prérequis. */
    PREPARE,
    /** Entretenir son niveau : les notions intermédiaires et expertes d'abord, par priorité ; les débutantes en dernier. */
    MAINTAIN;

    /** Le parcours d'un sujet qu'on ne suit pas, ou révisé avec un groupe. */
    public static final LearningTrack DEFAULT = PREPARE;
}
