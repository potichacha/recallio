package com.recallio.content;

import java.util.Set;

/** Format d'une carte (CLAUDE.md, section 5.3). Stocké tel quel en base : ne pas renommer une valeur existante. */
public enum CardType {
    /** Question technique courte, réponse en auto-évaluation. */
    FLASHCARD,
    /** « Qu'affiche ce code ? » : auto-évaluation en V1. */
    CODE_OUTPUT,
    /** Mise en situation : rédaction libre puis comparaison avec la réponse modèle. */
    OPEN_SCENARIO,
    /** Terme du glossaire et sa définition. */
    DEFINITION,
    /** Question à choix multiples, corrigée par le serveur (ADR 0019). */
    MULTIPLE_CHOICE,
    /** Une affirmation, vraie ou fausse : un QCM à deux choix fixes, corrigé par le serveur (ADR 0027). */
    TRUE_FALSE,
    /** Un code bogué à corriger : rédaction libre puis comparaison avec la correction modèle (ADR 0027). */
    BUG_FIX,
    /** Du code à trous, chaque trou corrigé par le serveur (ADR 0027). */
    FILL_IN,
    /** Un exercice de code en plusieurs fichiers (ADR 0029), jugé par ses tests dans le bac à sable (ADR 0030). */
    CODING_EXERCISE;

    /**
     * Les formats corrigés par le serveur d'après les choix cochés : seuls admis là où la triche ou la rapidité compte
     * (défis de groupe, parties live).
     */
    public static final Set<CardType> GRADED_BY_CHOICES = Set.of(MULTIPLE_CHOICE, TRUE_FALSE);

    /**
     * Les formats que le serveur corrige lui-même (les choix cochés, les trous, les tests d'un exercice) : leur réussite
     * est vérifiée, et non déclarée par l'utilisateur. Elle rapporte un bonus d'XP (ADR 0035).
     */
    public static final Set<CardType> GRADED_BY_SERVER = Set.of(MULTIPLE_CHOICE, TRUE_FALSE, FILL_IN, CODING_EXERCISE);

    public boolean isGradedByChoices() {
        return GRADED_BY_CHOICES.contains(this);
    }

    public boolean isGradedByServer() {
        return GRADED_BY_SERVER.contains(this);
    }
}
