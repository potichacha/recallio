package com.recallio.gamification.domain;

import com.recallio.content.CardType;

/**
 * L'effort que demande un format (ADR 0035) : il multiplie l'XP de base d'une révision. Les formats exigeants
 * rapportent davantage ; la vitesse, jamais.
 */
public enum Effort {
    /** Retrouver une réponse courte : une question de cours, une définition, un vrai ou faux, un QCM. */
    SHORT(1),
    /** Lire ou compléter du code : « qu'affiche ce code ? », un code à compléter. */
    CODE(1.5),
    /** Rédiger une réponse : une mise en situation, la correction d'un bug. */
    WRITING(2),
    /** Écrire un programme qui passe ses tests. */
    EXERCISE(4);

    private final double multiplier;

    Effort(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    /** Un nouveau format doit trouver sa place ici : sans lui, le {@code switch} ne compile pas. */
    public static Effort of(CardType type) {
        return switch (type) {
            case FLASHCARD, DEFINITION, TRUE_FALSE, MULTIPLE_CHOICE -> SHORT;
            case CODE_OUTPUT, FILL_IN -> CODE;
            case OPEN_SCENARIO, BUG_FIX -> WRITING;
            case CODING_EXERCISE -> EXERCISE;
        };
    }
}
