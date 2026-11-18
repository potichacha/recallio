package com.recallio.content;

import java.util.Set;

/**
 * Format d'une carte (CLAUDE.md, section 5.3). Stocké tel quel en base : ne pas renommer une valeur existante.
 *
 * <p>Le serveur corrige chaque carte qu'on révise (ADR 0063). Ce qui le permet dépend de la carte, plus de son format :
 * ses choix, ses trous ou ses tests. Une carte d'un document Markdown sans choix ne se révise pas : elle attend sa
 * réécriture en dossier de sujet (ADR 0050).
 */
public enum CardType {
    /** Question de cours, corrigée sur ses choix (ADR 0051). */
    FLASHCARD,
    /** « Qu'affiche ce code ? », corrigé sur ses choix, des sorties (ADR 0051). */
    CODE_OUTPUT,
    /** Mise en situation, corrigée sur ses choix, des façons d'y répondre (ADR 0051). */
    OPEN_SCENARIO,
    /** Terme du glossaire, corrigé sur ses choix, des définitions (ADR 0051). */
    DEFINITION,
    /** Question à choix multiples, corrigée par le serveur (ADR 0019). */
    MULTIPLE_CHOICE,
    /** Une affirmation, vraie ou fausse : un QCM à deux choix fixes, corrigé par le serveur (ADR 0027). */
    TRUE_FALSE,
    /** Un code bogué à corriger (ADR 0027), corrigé sur ses choix, des corrections (ADR 0051). */
    BUG_FIX,
    /** Du code à trous, chaque trou corrigé par le serveur (ADR 0027). */
    FILL_IN,
    /** Un exercice de code en plusieurs fichiers (ADR 0029), jugé par ses tests dans le bac à sable (ADR 0030). */
    CODING_EXERCISE;

    /**
     * Les formats que le serveur corrige toujours, quelle que soit la carte : un QCM et un vrai ou faux ont toujours
     * leurs choix, un code à trous ses trous, un exercice ses tests. Les autres formats le sont quand la carte a ses
     * choix : voir {@link CardDirectory.CardSnapshot#isGradedByServer()}.
     */
    public static final Set<CardType> GRADED_BY_SERVER = Set.of(MULTIPLE_CHOICE, TRUE_FALSE, FILL_IN, CODING_EXERCISE);

    public boolean isGradedByServer() {
        return GRADED_BY_SERVER.contains(this);
    }
}
