package com.recallio.gamification.domain;

import com.recallio.content.CardType;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Barème de l'XP et objectif quotidien (CLAUDE.md, section 6.3 ; ADR 0035).
 *
 * <p>Chaque révision rapporte une XP de base, quelle que soit la note : récompenser « Facile » pousserait à surévaluer
 * ses réponses. Cette base dépend de l'effort du format ({@link Effort}). Une réussite corrigée par le serveur rapporte
 * en plus la moitié de la base, et rien ne récompense la vitesse. Atteindre l'objectif du jour rapporte un bonus, une
 * seule fois par jour, qui n'appartient à aucun sujet.
 *
 * @param xpPerReview l'unité du barème : l'XP d'une révision d'effort {@link Effort#SHORT}
 * @param dailyGoal nombre de cartes à réviser dans la journée pour qu'elle compte dans le streak
 */
public record XpRules(int xpPerReview, int dailyGoal, int dailyGoalBonus) {

    public XpRules {
        if (xpPerReview < 0 || dailyGoalBonus < 0) {
            throw new IllegalArgumentException("L'XP attribuée ne peut pas être négative");
        }
        if (dailyGoal < 1) {
            throw new IllegalArgumentException("L'objectif quotidien doit être d'au moins une carte");
        }
    }

    /**
     * L'XP gagnée par une révision.
     *
     * @param cardsReviewedToday nombre de cartes révisées aujourd'hui, celle-ci comprise
     */
    public List<XpAward> awardsFor(ReviewedCard card, int cardsReviewedToday) {
        List<XpAward> awards = new ArrayList<>();
        long base = Math.round(xpPerReview * Effort.of(card.type()).multiplier());
        award(awards, XpReason.REVIEW, base, card.subjectSlug());
        if (card.verifiedSuccess()) {
            award(awards, XpReason.VERIFIED_SUCCESS, Math.round(base / 2.0), card.subjectSlug());
        }
        // Le compteur avance d'une carte à la fois : l'égalité ne se produit qu'une fois par jour.
        if (cardsReviewedToday == dailyGoal) {
            award(awards, XpReason.DAILY_GOAL, dailyGoalBonus, null);
        }
        return List.copyOf(awards);
    }

    /** Une entrée du registre est toujours positive : une XP nulle (barème à zéro) n'est pas attribuée. */
    private static void award(List<XpAward> awards, XpReason reason, long amount, @Nullable String subjectSlug) {
        if (amount > 0) {
            awards.add(new XpAward(reason, Math.toIntExact(amount), subjectSlug));
        }
    }

    /**
     * @param gradedByServer si le serveur a corrigé la réponse (ses choix, ses trous ou ses tests)
     * @param recalled si la carte est réussie : notée au moins « Bien », ou juste pour une carte corrigée par le serveur
     */
    public record ReviewedCard(String subjectSlug, CardType type, boolean gradedByServer, boolean recalled) {

        /** Une réussite vérifiée par le serveur, et non déclarée par l'utilisateur. */
        boolean verifiedSuccess() {
            return recalled && gradedByServer;
        }
    }

    /** @param subjectSlug le sujet qui gagne l'XP ; absent pour le bonus de l'objectif du jour */
    public record XpAward(
            XpReason reason, int amount, @Nullable String subjectSlug) {}
}
