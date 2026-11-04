package com.recallio.review.domain.track;

/** Le niveau de maîtrise d'une notion (ADR 0034), du moins au plus avancé. Exposé tel quel par l'API. */
public enum MasteryLevel {
    /** Aucune carte de la notion vue. */
    NEW,
    /** Au moins une carte vue. */
    DISCOVERED,
    /** Assez de cartes réussies : la règle « acquise » du parcours (ADR 0032). */
    ACQUIRED,
    /** Des cartes maîtrisées, dans plusieurs formats. */
    MASTERED,
    /** Des cartes retenues sur la durée, dans plusieurs formats, et chaque format de la notion réussi. */
    EXPERT
}
