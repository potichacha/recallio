package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.track.LearningPath;
import com.recallio.review.domain.track.LearningPath.NotionStatus;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : la carte du parcours d'un sujet (ADR 0032), pour l'utilisateur connecté. Chaque notion, dans l'ordre du
 * référentiel, avec son état (à rédiger, verrouillée, disponible, en cours ou acquise) et sa maîtrise (ADR 0034).
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class LearningPathService {

    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final FollowedSubjects followedSubjects;
    private final ReviewStateRepository states;
    private final ReviewLogRepository log;

    public LearningPathService(
            CardDirectory cards,
            SubjectDirectory subjects,
            FollowedSubjects followedSubjects,
            ReviewStateRepository states,
            ReviewLogRepository log) {
        this.cards = cards;
        this.subjects = subjects;
        this.followedSubjects = followedSubjects;
        this.states = states;
        this.log = log;
    }

    /** @throws com.recallio.platform.domain.ResourceNotFoundException si aucun sujet publié ne porte ce slug */
    public SubjectPath path(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        List<NotionInfo> notions = cards.notions(Set.of(subjectSlug));
        Map<NotionInfo, NotionMastery> mastery = mastery(user, subjectSlug, notions);
        Map<UUID, Integer> recalled = new HashMap<>();
        mastery.forEach((notion, notionMastery) -> recalled.put(notion.id(), notionMastery.recalled()));
        var path = new LearningPath(notions, recalled);
        LearningTrack track = followedSubjects.tracksOf(user).getOrDefault(subjectSlug, LearningTrack.DEFAULT);
        boolean prerequisitesApply = track == LearningTrack.LEARN;
        List<NotionStep> steps = mastery.entrySet().stream()
                .map(entry -> {
                    NotionInfo notion = entry.getKey();
                    NotionMastery notionMastery = entry.getValue();
                    NotionStatus status = path.status(notion.id(), notionMastery.seen(), prerequisitesApply);
                    return new NotionStep(notion, status, notionMastery);
                })
                .toList();
        return new SubjectPath(subjectSlug, track, steps);
    }

    /**
     * La maîtrise de chaque notion du sujet, dans l'ordre du référentiel. Les réussites comptent les cartes retirées
     * depuis, comme la file de révision le fait pour débloquer les notions : la notion acquise est la même partout.
     */
    private Map<NotionInfo, NotionMastery> mastery(UserRef user, String subjectSlug, List<NotionInfo> notions) {
        Map<UUID, Map<UUID, CardType>> cardsOfNotion = new HashMap<>();
        for (ActiveCard card : cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), Set.of()))) {
            UUID notion = card.notionId();
            if (notion != null) {
                cardsOfNotion.computeIfAbsent(notion, _ -> new HashMap<>()).put(card.cardId(), card.type());
            }
        }
        Map<UUID, Set<UUID>> recalledOfNotion = new HashMap<>();
        cards.notionsOf(log.cardsRecalled(user.id()))
                .forEach((card, notion) -> recalledOfNotion
                        .computeIfAbsent(notion, _ -> new HashSet<>())
                        .add(card));
        Map<UUID, ReviewState> memory = states.findAllByUser(user.id());
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

    /** @param notions les notions du sujet, dans l'ordre du référentiel */
    public record SubjectPath(String subjectSlug, LearningTrack track, List<NotionStep> notions) {

        public SubjectPath {
            notions = List.copyOf(notions);
        }
    }

    /** @param mastery la maîtrise de la notion, avec ses cartes vues */
    public record NotionStep(NotionInfo notion, NotionStatus status, NotionMastery mastery) {}
}
