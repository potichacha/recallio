package com.recallio.feedback.domain;

/** Ce qui ne va pas dans le contenu signalé (ADR 0042). Stocké par son nom : ne pas renommer une valeur existante. */
public enum ReportReason {
    /** La réponse, ou la correction, est fausse. */
    WRONG_ANSWER,
    /** La question, ou le texte, est ambiguë ou difficile à comprendre. */
    UNCLEAR,
    /** Le code ne compile pas, ou ne fait pas ce qui est annoncé. */
    BROKEN_CODE,
    /** Une faute de frappe, d'orthographe ou de mise en forme. */
    TYPO,
    OTHER
}
