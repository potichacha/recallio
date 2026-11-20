package com.recallio.review.domain.interview;

/**
 * Où en est la préparation d'un entretien (ADR 0066), par rapport au plan. Le premier statut qui s'applique l'emporte,
 * dans l'ordre de l'énumération. Exposé tel quel par l'API.
 */
public enum InterviewStatus {
    /** L'entretien est passé. */
    PASSED,
    /** L'indice de préparation dit « prêt » (ADR 0041). */
    READY,
    /** Une semaine passée n'est pas finie : des notions de son objectif restent à acquérir. */
    BEHIND,
    /** L'objectif de la semaine est déjà atteint, et il reste des notions. */
    AHEAD,
    /** Sinon : l'objectif de la semaine est en cours, ou toutes les notions sont acquises et restent à maîtriser. */
    ON_TRACK
}
