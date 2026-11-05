package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.review.SkillProfiles;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.tour.TourProgressRepository;
import com.recallio.review.domain.track.MasteryCounts;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.Readiness;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Les compétences d'un utilisateur, pour son profil public (ADR 0043) : le vérifié, puis le déclaré. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class SkillProfileService implements SkillProfiles {

    private final CardDirectory cards;
    private final ReviewStateRepository states;
    private final ReviewLogRepository log;
    private final NotionMasteries masteries;
    private final TourProgressRepository tour;

    public SkillProfileService(
            CardDirectory cards,
            ReviewStateRepository states,
            ReviewLogRepository log,
            NotionMasteries masteries,
            TourProgressRepository tour) {
        this.cards = cards;
        this.states = states;
        this.log = log;
        this.masteries = masteries;
        this.tour = tour;
    }

    @Override
    public List<SubjectSkills> of(UUID userId, List<String> subjectSlugs) {
        if (subjectSlugs.isEmpty()) {
            return List.of();
        }
        Set<String> subjects = Set.copyOf(subjectSlugs);
        Map<UUID, ReviewState> memory = states.findAllByUser(userId);
        Map<NotionInfo, NotionMastery> mastery = masteries.of(userId, subjects, memory);

        // Une carte réussie au moins une fois : pour un format corrigé par le serveur, c'est une réussite vérifiée.
        Set<UUID> recalled = log.cardsRecalled(userId);
        Map<String, Integer> exercises = new HashMap<>();
        Map<String, Integer> answers = new HashMap<>();
        for (ActiveCard card : cards.activeCards(new CardFilter(subjects, null, Set.of(), Set.of()))) {
            if (!recalled.contains(card.cardId()) || !card.type().isGradedByServer()) {
                continue;
            }
            var counts = card.type() == CardType.CODING_EXERCISE ? exercises : answers;
            counts.merge(card.subjectSlug(), 1, Integer::sum);
        }
        Set<UUID> validated = tour.validated(
                userId,
                mastery.keySet().stream()
                        .filter(NotionInfo::isInTour)
                        .map(NotionInfo::id)
                        .toList());

        Map<String, List<Map.Entry<NotionInfo, NotionMastery>>> notionsBySubject = mastery.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().subjectSlug()));
        return subjectSlugs.stream()
                .map(subject -> {
                    var notions = notionsBySubject.getOrDefault(subject, List.of());
                    var verified = new Verified(
                            exercises.getOrDefault(subject, 0), answers.getOrDefault(subject, 0), (int) notions.stream()
                                    .filter(entry ->
                                            validated.contains(entry.getKey().id()))
                                    .count());
                    return new SubjectSkills(subject, verified, declared(notions));
                })
                .toList();
    }

    private static Declared declared(List<Map.Entry<NotionInfo, NotionMastery>> notions) {
        MasteryCounts counts =
                MasteryCounts.of(notions.stream().map(Map.Entry::getValue).toList());
        Readiness readiness = Readiness.of(notions.stream()
                .filter(entry -> entry.getKey().priority() == Readiness.ESSENTIAL_PRIORITY)
                .map(Map.Entry::getValue)
                .toList());
        return new Declared(counts.total(), counts.acquired(), counts.mastered(), counts.expert(), readiness.percent());
    }
}
