package com.recallio.feedback.domain;

/** Comment un admin a traité un signalement (ADR 0042). Stocké par son nom : ne pas renommer une valeur existante. */
public enum ReportResolution {
    /** Le contenu a été corrigé, ou le sera par une pull request de contenu. */
    FIXED,
    /** Le contenu est juste : rien à corriger. */
    REJECTED
}
