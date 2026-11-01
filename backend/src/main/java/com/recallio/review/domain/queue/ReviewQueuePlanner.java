package com.recallio.review.domain.queue;

import com.recallio.review.domain.ReviewState;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Compose la file de révision (CLAUDE.md, section 6.1) : d'abord les cartes dues, de la plus en retard à la moins en
 * retard, quel que soit leur sujet, puis des cartes nouvelles, sans dépasser le quota quotidien de nouvelles cartes.
 *
 * <p>Les nouvelles cartes alternent entre les sujets (ADR 0014) : le quota est partagé à parts égales, et la part d'un
 * sujet qui n'a plus de cartes nouvelles revient aux autres. Dans chaque sujet, elles suivent l'ordre du programme.
 */
public final class ReviewQueuePlanner {

    private ReviewQueuePlanner() {}

    /**
     * @param eligibleCards cartes actives correspondant au filtre (sujets, thème, format), dans l'ordre du programme
     * @param states mémoire de l'utilisateur, par carte (les cartes absentes sont nouvelles)
     * @param newCardsAllowed nouvelles cartes encore autorisées aujourd'hui
     * @param size nombre maximal de cartes renvoyées
     */
    public static QueuePlan plan(
            List<Candidate> eligibleCards, Map<UUID, ReviewState> states, Instant now, int newCardsAllowed, int size) {
        List<Scheduled> scheduled = eligibleCards.stream()
                .flatMap(card ->
                        Stream.ofNullable(states.get(card.cardId())).map(state -> new Scheduled(card.cardId(), state)))
                .toList();
        List<UUID> due = scheduled.stream()
                .filter(card -> card.state().isDue(now))
                .sorted(Comparator.comparing(card -> card.state().dueAt()))
                .map(Scheduled::cardId)
                .toList();
        List<UUID> fresh = alternateSubjects(
                eligibleCards.stream()
                        .filter(card -> !states.containsKey(card.cardId()))
                        .toList(),
                Math.max(0, newCardsAllowed));
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

    /** Prend une carte de chaque sujet à tour de rôle, dans l'ordre des sujets, jusqu'à {@code limit} cartes. */
    private static List<UUID> alternateSubjects(List<Candidate> fresh, int limit) {
        Map<String, Deque<UUID>> bySubject = new LinkedHashMap<>();
        fresh.forEach(card -> bySubject
                .computeIfAbsent(card.subjectSlug(), _ -> new ArrayDeque<>())
                .add(card.cardId()));
        List<UUID> picked = new ArrayList<>();
        while (picked.size() < limit && !bySubject.isEmpty()) {
            Iterator<Deque<UUID>> subjects = bySubject.values().iterator();
            while (subjects.hasNext() && picked.size() < limit) {
                Deque<UUID> cards = subjects.next();
                picked.add(cards.removeFirst());
                if (cards.isEmpty()) {
                    subjects.remove();
                }
            }
        }
        return picked;
    }

    /** Une carte éligible et son sujet. */
    public record Candidate(UUID cardId, String subjectSlug) {}

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
