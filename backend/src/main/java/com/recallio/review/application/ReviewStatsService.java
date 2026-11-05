package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ThemeCards;
import com.recallio.content.NotionInfo;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.stats.CardCounts;
import com.recallio.review.domain.track.MasteryCounts;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.Readiness;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le tableau de bord de révision (cartes dues, progression par sujet puis par thème, maîtrise des notions
 * et indice de préparation de chaque sujet), sur les sujets suivis : les mêmes cartes que la file de révision sans
 * filtre.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ReviewStatsService {

    private final CardDirectory cards;
    private final FollowedSubjects followedSubjects;
    private final ReviewStateRepository states;
    private final NewCardQuota quota;
    private final NotionMasteries masteries;
    private final Clock clock;

    public ReviewStatsService(
            CardDirectory cards,
            FollowedSubjects followedSubjects,
            ReviewStateRepository states,
            NewCardQuota quota,
            NotionMasteries masteries,
            Clock clock) {
        this.cards = cards;
        this.followedSubjects = followedSubjects;
        this.states = states;
        this.quota = quota;
        this.masteries = masteries;
        this.clock = clock;
    }

    public ReviewStats stats(UserRef user) {
        Instant now = clock.instant();
        Instant endOfDay = NewCardQuota.startOfNextDay(now, user.timezone());
        Map<UUID, ReviewState> memory = states.findAllByUser(user.id());
        List<String> followed = followedSubjects.of(user);
        List<ThemeCards> themes = cards.activeCardsByTheme().stream()
                .filter(theme -> followed.contains(theme.subjectSlug()))
                .toList();

        // Les thèmes arrivent groupés par sujet, dans l'ordre du catalogue : le regroupement garde cet ordre.
        Map<String, List<ThemeCards>> bySubject = themes.stream()
                .collect(Collectors.groupingBy(ThemeCards::subjectSlug, LinkedHashMap::new, Collectors.toList()));
        Map<NotionInfo, NotionMastery> mastery = masteries.of(user.id(), Set.copyOf(followed), memory);
        Map<String, MasteryCounts> notions = notionsBySubject(mastery);
        Map<String, Readiness> readiness = readinessBySubject(mastery);
        Map<String, Integer> newAllowed = quota.remainingToday(user, followed, now);
        List<SubjectStats> perSubject = bySubject.values().stream()
                .map(subjectThemes ->
                        subjectStats(subjectThemes, memory, now, endOfDay, notions, readiness, newAllowed))
                .toList();
        CardCounts overall = CardCounts.of(cardIds(themes), memory, now, endOfDay);
        int newToday = perSubject.stream().mapToInt(SubjectStats::newToday).sum();
        return new ReviewStats(overall, newToday, perSubject);
    }

    private static SubjectStats subjectStats(
            List<ThemeCards> themes,
            Map<UUID, ReviewState> memory,
            Instant now,
            Instant endOfDay,
            Map<String, MasteryCounts> notions,
            Map<String, Readiness> readiness,
            Map<String, Integer> newAllowed) {
        List<ThemeStats> perTheme = themes.stream()
                .map(theme -> new ThemeStats(
                        theme.slug(), theme.title(), CardCounts.of(theme.cardIds(), memory, now, endOfDay)))
                .toList();
        ThemeCards first = themes.getFirst();
        CardCounts counts = CardCounts.of(cardIds(themes), memory, now, endOfDay);
        return new SubjectStats(
                first.subjectSlug(),
                first.subjectTitle(),
                counts,
                Math.min(newAllowed.getOrDefault(first.subjectSlug(), 0), counts.total() - counts.seen()),
                notions.getOrDefault(first.subjectSlug(), MasteryCounts.NONE),
                readiness.getOrDefault(first.subjectSlug(), Readiness.NONE),
                perTheme);
    }

    /** La maîtrise des notions de chaque sujet (ADR 0034) ; un sujet décrit par un document Markdown n'en a pas. */
    private static Map<String, MasteryCounts> notionsBySubject(Map<NotionInfo, NotionMastery> mastery) {
        return mastery.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().subjectSlug(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList()), MasteryCounts::of)));
    }

    /** L'indice de préparation de chaque sujet (ADR 0041), d'après ses seules notions essentielles. */
    private static Map<String, Readiness> readinessBySubject(Map<NotionInfo, NotionMastery> mastery) {
        return mastery.entrySet().stream()
                .filter(entry -> entry.getKey().priority() == Readiness.ESSENTIAL_PRIORITY)
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().subjectSlug(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList()), Readiness::of)));
    }

    private static List<UUID> cardIds(List<ThemeCards> themes) {
        return themes.stream().flatMap(theme -> theme.cardIds().stream()).toList();
    }

    /**
     * @param newToday nouvelles cartes encore proposées aujourd'hui, tous sujets suivis confondus
     * @param subjects les sujets suivis, dans l'ordre du catalogue
     */
    public record ReviewStats(CardCounts overall, int newToday, List<SubjectStats> subjects) {}

    /**
     * @param newToday nouvelles cartes encore proposées aujourd'hui dans ce sujet (son quota restant, ADR 0048, dans la
     *     limite de ses cartes jamais vues)
     * @param notions les notions du sujet par niveau de maîtrise ; aucune pour un sujet sans référentiel
     * @param readiness son indice de préparation ; vide pour un sujet sans référentiel
     * @param themes les thèmes du sujet, dans l'ordre du document
     */
    public record SubjectStats(
            String slug,
            String title,
            CardCounts counts,
            int newToday,
            MasteryCounts notions,
            Readiness readiness,
            List<ThemeStats> themes) {}

    public record ThemeStats(String slug, String title, CardCounts counts) {}
}
