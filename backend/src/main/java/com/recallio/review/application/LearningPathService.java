package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.track.LearningPath;
import com.recallio.review.domain.track.LearningPath.NotionStatus;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage : la carte du parcours d'un sujet (ADR 0032), pour l'utilisateur connecté. Chaque notion, dans l'ordre du
 * référentiel, avec son état : à rédiger, verrouillée, disponible, en cours ou acquise.
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
        Map<UUID, Integer> seen = perNotion(states.findAllByUser(user.id()).keySet());
        var path = new LearningPath(notions, perNotion(log.cardsRecalled(user.id())));
        LearningTrack track = followedSubjects.tracksOf(user).getOrDefault(subjectSlug, LearningTrack.DEFAULT);
        boolean prerequisitesApply = track == LearningTrack.LEARN;
        List<NotionStep> steps = notions.stream()
                .map(notion -> new NotionStep(
                        notion,
                        path.status(notion.id(), seen.getOrDefault(notion.id(), 0), prerequisitesApply),
                        seen.getOrDefault(notion.id(), 0)))
                .toList();
        return new SubjectPath(subjectSlug, track, steps);
    }

    /** Ces cartes, comptées par notion. */
    private Map<UUID, Integer> perNotion(Collection<UUID> cardIds) {
        return cards.notionsOf(cardIds).values().stream()
                .collect(Collectors.toMap(Function.identity(), _ -> 1, Integer::sum));
    }

    /** @param notions les notions du sujet, dans l'ordre du référentiel */
    public record SubjectPath(String subjectSlug, LearningTrack track, List<NotionStep> notions) {

        public SubjectPath {
            notions = List.copyOf(notions);
        }
    }

    /** @param seen les cartes de la notion déjà vues */
    public record NotionStep(NotionInfo notion, NotionStatus status, int seen) {}
}
