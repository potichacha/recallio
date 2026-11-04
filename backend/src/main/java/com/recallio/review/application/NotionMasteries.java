package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.track.NotionMastery;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La maîtrise des notions d'un utilisateur (ADR 0034), pour la carte du parcours et le tableau de bord.
 *
 * <p>Les réussites comptent les cartes retirées depuis, comme la file de révision le fait pour débloquer les notions :
 * une notion acquise l'est partout de la même façon.
 */
@ApplicationScoped
public class NotionMasteries {

    private final CardDirectory cards;
    private final ReviewLogRepository log;

    public NotionMasteries(CardDirectory cards, ReviewLogRepository log) {
        this.cards = cards;
        this.log = log;
    }

    /**
     * @param memory la mémoire de l'utilisateur, déjà lue par l'appelant
     * @return la maîtrise de chaque notion de ces sujets, dans l'ordre de leurs référentiels
     */
    public Map<NotionInfo, NotionMastery> of(UUID userId, Set<String> subjectSlugs, Map<UUID, ReviewState> memory) {
        List<NotionInfo> notions = cards.notions(subjectSlugs);
        if (notions.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Map<UUID, CardType>> cardsOfNotion = new HashMap<>();
        for (ActiveCard card : cards.activeCards(new CardFilter(subjectSlugs, null, Set.of(), Set.of()))) {
            UUID notion = card.notionId();
            if (notion != null) {
                cardsOfNotion.computeIfAbsent(notion, _ -> new HashMap<>()).put(card.cardId(), card.type());
            }
        }
        Map<UUID, Set<UUID>> recalledOfNotion = new HashMap<>();
        cards.notionsOf(log.cardsRecalled(userId))
                .forEach((card, notion) -> recalledOfNotion
                        .computeIfAbsent(notion, _ -> new HashSet<>())
                        .add(card));
        Map<NotionInfo, NotionMastery> mastery = new LinkedHashMap<>();
        for (NotionInfo notion : notions) {
            mastery.put(
                    notion,
                    NotionMastery.of(
                            cardsOfNotion.getOrDefault(notion.id(), Map.of()),
                            memory,
                            recalledOfNotion.getOrDefault(notion.id(), Set.of())));
        }
        return mastery;
    }
}
