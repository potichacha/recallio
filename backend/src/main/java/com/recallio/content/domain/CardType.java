package com.recallio.content.domain;

/** Format d'une carte (CLAUDE.md, section 5.3). Stocké tel quel en base : ne pas renommer une valeur existante. */
public enum CardType {
    /** Question technique courte, réponse en auto-évaluation. */
    FLASHCARD,
    /** « Qu'affiche ce code ? » : auto-évaluation en V1. */
    CODE_OUTPUT,
    /** Mise en situation : rédaction libre puis comparaison avec la réponse modèle. */
    OPEN_SCENARIO,
    /** Terme du glossaire et sa définition. */
    DEFINITION
}
