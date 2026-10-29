package com.recallio.review.domain.stats;

import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewState;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Où en est un utilisateur sur un ensemble de cartes (un thème, ou tout le catalogue).
 *
 * @param seen cartes révisées au moins une fois
 * @param mastered cartes en révision dont la stabilité atteint {@link #MASTERED_STABILITY_DAYS} jours
 * @param dueNow cartes à revoir maintenant
 * @param dueToday cartes à revoir d'ici la fin de la journée (dont {@code dueNow})
 */
public record CardCounts(int total, int seen, int mastered, int dueNow, int dueToday) {

    /**
     * Seuil au-delà duquel une carte est considérée comme maîtrisée : trois semaines de stabilité, soit le seuil des
     * cartes « matures » d'Anki.
     */
    public static final double MASTERED_STABILITY_DAYS = 21;

    /**
     * @param cardIds les cartes à compter (cartes actives)
     * @param states la mémoire de l'utilisateur ; les cartes absentes sont nouvelles
     * @param endOfDay minuit suivant, dans le fuseau de l'utilisateur
     */
    public static CardCounts of(
            Collection<UUID> cardIds, Map<UUID, ReviewState> states, Instant now, Instant endOfDay) {
        var known = cardIds.stream()
                .flatMap(cardId -> Stream.ofNullable(states.get(cardId)))
                .toList();
        return new CardCounts(
                cardIds.size(),
                known.size(),
                (int) known.stream().filter(CardCounts::isMastered).count(),
                (int) known.stream().filter(state -> state.isDue(now)).count(),
                (int) known.stream()
                        .filter(state -> state.dueAt().isBefore(endOfDay))
                        .count());
    }

    private static boolean isMastered(ReviewState state) {
        return state.state() == LearningState.REVIEW && state.stability() >= MASTERED_STABILITY_DAYS;
    }
}
