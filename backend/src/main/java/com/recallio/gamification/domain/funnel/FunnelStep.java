package com.recallio.gamification.domain.funnel;

/** Les étapes du parcours d'un visiteur (ADR 0064), dans l'ordre où il les franchit. */
public enum FunnelStep {
    /** La page publique ouverte, une fois par session de navigateur. */
    VISIT,
    /** Un essai sans compte commencé : la réponse à sa première question. */
    TRIAL,
    /** Un compte créé : le profil, à la première connexion. */
    ACCOUNT,
    /** La toute première carte révisée d'un utilisateur. */
    FIRST_REVIEW
}
