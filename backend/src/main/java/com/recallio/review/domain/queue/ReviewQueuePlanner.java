package com.recallio.review.domain.queue;

import com.recallio.review.domain.ReviewState;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Compose la file de révision (CLAUDE.md, section 6.1) : d'abord les cartes dues, de la plus en retard à la moins en
 * retard, puis des cartes nouvelles dans l'ordre du programme, sans dépasser le quota quotidien de nouvelles cartes.
 */
public final class ReviewQueuePlanner {

    private ReviewQueuePlanner() {}

    /**
     * @param eligibleCards cartes actives correspondant au filtre (thème, format), dans l'ordre du programme
     * @param states mémoire de l'utilisateur, par carte (les cartes absentes sont nouvelles)
     * @param newCardsAllowed nouvelles cartes encore autorisées aujourd'hui
     * @param size nombre maximal de cartes renvoyées
     */
    public static QueuePlan plan(
            List<UUID> eligibleCards, Map<UUID, ReviewState> states, Instant now, int newCardsAllowed, int size) {
        List<Scheduled> scheduled = eligibleCards.stream()
                .flatMap(cardId -> Stream.ofNullable(states.get(cardId)).map(state -> new Scheduled(cardId, state)))
                .toList();
        List<UUID> due = scheduled.stream()
                .filter(card -> card.state().isDue(now))
                .sorted(Comparator.comparing(card -> card.state().dueAt()))
                .map(Scheduled::cardId)
                .toList();
        List<UUID> fresh = eligibleCards.stream()
                .filter(cardId -> !states.containsKey(cardId))
                .limit(Math.max(0, newCardsAllowed))
                .toList();
        @Nullable
        Instant nextDueAt = scheduled.stream()
                .map(card -> card.state().dueAt())
                .filter(dueAt -> dueAt.isAfter(now))
                .min(Comparator.naturalOrder())
                .orElse(null);
        List<UUID> cards =
                Stream.concat(due.stream(), fresh.stream()).limit(size).toList();
        return new QueuePlan(cards, due.size(), fresh.size(), nextDueAt);
    }

    /**
     * @param cardIds les cartes à présenter, dans l'ordre
     * @param dueCount nombre total de cartes dues (la file peut en contenir moins)
     * @param newCount nombre total de cartes nouvelles disponibles aujourd'hui
     * @param nextDueAt prochaine échéance future, pour dire « revenez dans 10 minutes » quand il n'y a plus rien
     */
    public record QueuePlan(
            List<UUID> cardIds,
            int dueCount,
            int newCount,
            @Nullable Instant nextDueAt) {

        public QueuePlan {
            cardIds = List.copyOf(cardIds);
        }
    }

    private record Scheduled(UUID cardId, ReviewState state) {}
}
