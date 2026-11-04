package com.recallio.review.application;

import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.track.LearningPath;
import com.recallio.review.domain.track.LearningPath.NotionStatus;
import com.recallio.review.domain.track.MasteryCounts;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : la carte du parcours d'un sujet (ADR 0032), pour l'utilisateur connecté. Ses domaines et leurs
 * chapitres, dans l'ordre du référentiel ; chaque notion avec son état (à rédiger, verrouillée, disponible, en cours ou
 * acquise) et sa maîtrise (ADR 0034).
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class LearningPathService {

    private final SubjectDirectory subjects;
    private final FollowedSubjects followedSubjects;
    private final ReviewStateRepository states;
    private final NotionMasteries masteries;

    public LearningPathService(
            SubjectDirectory subjects,
            FollowedSubjects followedSubjects,
            ReviewStateRepository states,
            NotionMasteries masteries) {
        this.subjects = subjects;
        this.followedSubjects = followedSubjects;
        this.states = states;
        this.masteries = masteries;
    }

    /** @throws com.recallio.platform.domain.ResourceNotFoundException si aucun sujet publié ne porte ce slug */
    public SubjectPath path(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        Map<NotionInfo, NotionMastery> mastery =
                masteries.of(user.id(), Set.of(subjectSlug), states.findAllByUser(user.id()));
        Map<UUID, Integer> recalled = new HashMap<>();
        mastery.forEach((notion, notionMastery) -> recalled.put(notion.id(), notionMastery.recalled()));
        var path = new LearningPath(mastery.keySet(), recalled);
        LearningTrack track = followedSubjects.tracksOf(user).getOrDefault(subjectSlug, LearningTrack.DEFAULT);
        boolean prerequisitesApply = track == LearningTrack.LEARN;

        var domains = new Grouping();
        mastery.forEach((notion, notionMastery) -> domains.add(new NotionStep(
                notion, path.status(notion.id(), notionMastery.seen(), prerequisitesApply), notionMastery)));
        return new SubjectPath(subjectSlug, track, MasteryCounts.of(mastery.values()), domains.result());
    }

    /** Range les notions, déjà dans l'ordre du référentiel, par domaine puis par chapitre. */
    private static final class Grouping {

        private final Map<String, List<NotionStep>> stepsOfDomain = new LinkedHashMap<>();

        void add(NotionStep step) {
            stepsOfDomain
                    .computeIfAbsent(step.notion().domain(), _ -> new ArrayList<>())
                    .add(step);
        }

        List<PathDomain> result() {
            return stepsOfDomain.values().stream().map(Grouping::domain).toList();
        }

        private static PathDomain domain(List<NotionStep> steps) {
            Map<String, List<NotionStep>> stepsOfChapter = new LinkedHashMap<>();
            steps.forEach(step -> stepsOfChapter
                    .computeIfAbsent(step.notion().chapter(), _ -> new ArrayList<>())
                    .add(step));
            NotionInfo first = steps.getFirst().notion();
            return new PathDomain(
                    first.domain(),
                    first.domainTitle(),
                    MasteryCounts.of(steps.stream().map(NotionStep::mastery).toList()),
                    stepsOfChapter.values().stream()
                            .map(chapterSteps -> {
                                NotionInfo notion = chapterSteps.getFirst().notion();
                                return new PathChapter(notion.chapter(), notion.chapterTitle(), chapterSteps);
                            })
                            .toList());
        }
    }

    /**
     * @param counts les notions du sujet par niveau de maîtrise
     * @param domains les domaines du référentiel, dans l'ordre ; vide pour un sujet sans référentiel
     */
    public record SubjectPath(String subjectSlug, LearningTrack track, MasteryCounts counts, List<PathDomain> domains) {

        public SubjectPath {
            domains = List.copyOf(domains);
        }
    }

    /** @param counts les notions du domaine par niveau de maîtrise */
    public record PathDomain(String id, String title, MasteryCounts counts, List<PathChapter> chapters) {

        public PathDomain {
            chapters = List.copyOf(chapters);
        }
    }

    public record PathChapter(String id, String title, List<NotionStep> notions) {

        public PathChapter {
            notions = List.copyOf(notions);
        }
    }

    /** @param mastery la maîtrise de la notion, avec ses cartes vues */
    public record NotionStep(NotionInfo notion, NotionStatus status, NotionMastery mastery) {}
}
