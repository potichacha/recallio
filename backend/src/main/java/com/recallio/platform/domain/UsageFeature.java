package com.recallio.platform.domain;

/**
 * Les fonctionnalités dont la page « Bêta » des admins mesure l'usage (ADR 0076). Le nom sort dans l'API : ne pas
 * renommer une valeur existante.
 *
 * <p>Une fonctionnalité est <em>datée</em> quand sa table garde l'instant de chaque usage : on compte alors les comptes
 * qui l'ont utilisée sur la période. Les autres sont des états sans histoire (un rappel accepté, un profil rendu
 * public) : on ne peut compter que les comptes qui les ont à ce jour.
 */
public enum UsageFeature {
    /** Une carte révisée. */
    REVIEW(true),
    /** Une notion validée dans le Tour. */
    TOUR(true),
    /** Un exercice de code rendu en révision. */
    CODING_EXERCISE(true),
    /** Un examen blanc commencé. */
    MOCK_EXAM(true),
    /** Une réponse à un défi du jour. */
    DAILY_CHALLENGE(true),
    /** Un duel classé ouvert ou rejoint. */
    RANKED_DUEL(true),
    /** Une partie live jouée jusqu'au bout. */
    LIVE_GAME(true),
    /** Une carte d'un défi de groupe reçue. */
    GROUP_CHALLENGE(true),
    /** Une quête du jour ou de la semaine terminée. */
    QUEST(true),
    /** Un test de niveau dont au moins un domaine est réussi. */
    LEVEL_CHECK(true),
    /** Une réponse corrigée en révision intensive (ADR 0078). */
    INTENSIVE_DRILL(true),
    /** Une date d'entretien fixée pour un sujet. */
    INTERVIEW_COUNTDOWN(false),
    /** Les rappels acceptés sur au moins un appareil. */
    REMINDERS(false),
    /** Un profil rendu public. */
    PUBLIC_PROFILE(false),
    /** L'appartenance à au moins un groupe. */
    GROUP(false);

    private final boolean dated;

    UsageFeature(boolean dated) {
        this.dated = dated;
    }

    /** Vrai si l'usage se borne à une période ; faux s'il ne se compte qu'à ce jour. */
    public boolean isDated() {
        return dated;
    }
}
