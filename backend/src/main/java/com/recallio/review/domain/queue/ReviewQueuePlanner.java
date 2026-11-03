package com.recallio.review.domain.queue;

import com.recallio.review.domain.ReviewState;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Compose la file de révision (CLAUDE.md, section 6.1) : d'abord les cartes dues, de la plus en retard à la moins en
 * retard, quel que soit leur sujet, puis des cartes nouvelles, sans dépasser le quota quotidien de nouvelles cartes.
 *
 * <p>Les nouvelles cartes alternent entre les sujets (ADR 0014) : le quota est partagé à parts égales, et la part d'un
 * sujet qui n'a plus de cartes nouvelles revient aux autres. Dans chaque sujet, elles suivent l'ordre du programme.
 *
 * <p>Une seule carte d'une même notion par jour (ADR 0031) : deux cartes sœurs révisées coup sur coup, la seconde l'est
 * avec la réponse de la première en tête. La file ne propose donc qu'une carte par notion, et aucune d'une notion dont
 * une autre carte a déjà été révisée aujourd'hui ; les cartes écartées attendent le lendemain. Une carte n'écarte
 * jamais elle-même : notée « À revoir », elle revient dans la journée.
 */
public final class ReviewQueuePlanner {

    private ReviewQueuePlanner() {}

    /**
     * @param eligibleCards cartes actives correspondant au filtre (sujets, thème, format), dans l'ordre du programme
     * @param states mémoire de l'utilisateur, par carte (les cartes absentes sont nouvelles)
     * @param reviewedToday les cartes déjà révisées aujourd'hui, par notion : leurs sœurs attendent le lendemain
     * @param newCardsAllowed nouvelles cartes encore autorisées aujourd'hui
     * @param size nombre maximal de cartes renvoyées
     */
    public static QueuePlan plan(
            List<Candidate> eligibleCards,
            Map<UUID, ReviewState> states,
            Map<UUID, Set<UUID>> reviewedToday,
            Instant now,
            int newCardsAllowed,
            int size) {
        var siblings = new Siblings(reviewedToday);
        List<Scheduled> scheduled = eligibleCards.stream()
                .flatMap(card -> Stream.ofNullable(states.get(card.cardId())).map(state -> new Scheduled(card, state)))
                .toList();
        List<UUID> due = scheduled.stream()
                .filter(card -> card.state().isDue(now))
                .sorted(Comparator.comparing(card -> card.state().dueAt()))
                .map(Scheduled::card)
                .filter(siblings::takeIfFirstOfItsNotion)
                .map(Candidate::cardId)
                .toList();
        List<UUID> fresh = alternateSubjects(
                eligibleCards.stream()
                        .filter(card -> !states.containsKey(card.cardId()))
                        .filter(siblings::takeIfFirstOfItsNotion)
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

    /**
     * Une carte éligible, son sujet et sa notion.
     *
     * @param notionId {@code null} pour une carte d'un document Markdown, qui n'a pas de sœurs
     */
    public record Candidate(
            UUID cardId, String subjectSlug, @Nullable UUID notionId) {

        /** Une carte sans notion. */
        public Candidate(UUID cardId, String subjectSlug) {
            this(cardId, subjectSlug, null);
        }
    }

    /** Les notions déjà prises : par une carte révisée aujourd'hui, ou par une carte déjà retenue dans la file. */
    private static final class Siblings {

        private final Map<UUID, Set<UUID>> reviewedToday;
        private final Set<UUID> taken = new HashSet<>();

        Siblings(Map<UUID, Set<UUID>> reviewedToday) {
            this.reviewedToday = reviewedToday;
        }

        /** Retient la carte si aucune de ses sœurs n'a été révisée aujourd'hui ni retenue avant elle. */
        boolean takeIfFirstOfItsNotion(Candidate card) {
            UUID notion = card.notionId();
            if (notion == null) {
                return true;
            }
            Set<UUID> reviewed = reviewedToday.getOrDefault(notion, Set.of());
            boolean siblingReviewed = reviewed.stream().anyMatch(other -> !other.equals(card.cardId()));
            return !siblingReviewed && taken.add(notion);
        }
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

    private record Scheduled(Candidate card, ReviewState state) {}
}
