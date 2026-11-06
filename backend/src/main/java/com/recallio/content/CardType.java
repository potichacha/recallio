package com.recallio.content;

import java.util.Set;

/** Format d'une carte (CLAUDE.md, section 5.3). Stocké tel quel en base : ne pas renommer une valeur existante. */
public enum CardType {
    /** Question de cours : corrigée sur ses choix, ou auto-évaluée tant qu'elle n'en a pas (ADR 0051). */
    FLASHCARD,
    /** « Qu'affiche ce code ? » : corrigé sur ses choix, ou auto-évalué tant qu'il n'en a pas (ADR 0051). */
    CODE_OUTPUT,
    /** Mise en situation : corrigée sur ses choix, ou rédigée puis comparée à la réponse modèle sans eux (ADR 0051). */
    OPEN_SCENARIO,
    /** Terme du glossaire et sa définition : corrigé sur ses choix, ou auto-évalué sans eux (ADR 0051). */
    DEFINITION,
    /** Question à choix multiples, corrigée par le serveur (ADR 0019). */
    MULTIPLE_CHOICE,
    /** Une affirmation, vraie ou fausse : un QCM à deux choix fixes, corrigé par le serveur (ADR 0027). */
    TRUE_FALSE,
    /**
     * Un code bogué à corriger (ADR 0027) : corrigé sur ses choix, ou rédigé puis comparé à la correction modèle sans eux
     * (ADR 0051).
     */
    BUG_FIX,
    /** Du code à trous, chaque trou corrigé par le serveur (ADR 0027). */
    FILL_IN,
    /** Un exercice de code en plusieurs fichiers (ADR 0029), jugé par ses tests dans le bac à sable (ADR 0030). */
    CODING_EXERCISE;

    /**
     * Les formats toujours corrigés d'après les choix cochés : seuls admis là où la triche ou la rapidité compte (défis
     * de groupe, parties live). Une carte d'un autre format qui a reçu des choix se corrige aussi sur eux (voir
     * {@link CardDirectory.CardSnapshot#isGradedByChoices()}) ; elle rejoindra ces parties à la fin de la migration
     * (ADR 0051).
     */
    public static final Set<CardType> GRADED_BY_CHOICES = Set.of(MULTIPLE_CHOICE, TRUE_FALSE);

    /**
     * Les formats que le serveur corrige toujours lui-même (les choix cochés, les trous, les tests d'un exercice) : leur
     * réussite est vérifiée, et non déclarée par l'utilisateur. Elle rapporte un bonus d'XP (ADR 0035). Une carte
     * d'un autre format qui a reçu des choix l'est aussi : voir {@link CardDirectory.CardSnapshot#isGradedByServer()}.
     */
    public static final Set<CardType> GRADED_BY_SERVER = Set.of(MULTIPLE_CHOICE, TRUE_FALSE, FILL_IN, CODING_EXERCISE);

    public boolean isGradedByChoices() {
        return GRADED_BY_CHOICES.contains(this);
    }

    public boolean isGradedByServer() {
        return GRADED_BY_SERVER.contains(this);
    }
}
