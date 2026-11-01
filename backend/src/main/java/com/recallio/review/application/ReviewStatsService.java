package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ThemeCards;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.stats.CardCounts;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le tableau de bord de révision (cartes dues, progression par sujet puis par thème), sur les sujets
 * suivis : les mêmes cartes que la file de révision sans filtre.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ReviewStatsService {

    private final CardDirectory cards;
    private final FollowedSubjects followedSubjects;
    private final ReviewStateRepository states;
    private final NewCardQuota quota;
    private final Clock clock;

    public ReviewStatsService(
            CardDirectory cards,
            FollowedSubjects followedSubjects,
            ReviewStateRepository states,
            NewCardQuota quota,
            Clock clock) {
        this.cards = cards;
        this.followedSubjects = followedSubjects;
        this.states = states;
        this.quota = quota;
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
        List<SubjectStats> perSubject = bySubject.values().stream()
                .map(subjectThemes -> subjectStats(subjectThemes, memory, now, endOfDay))
                .toList();
        CardCounts overall = CardCounts.of(cardIds(themes), memory, now, endOfDay);
        int newToday = Math.min(quota.remainingToday(user, now), overall.total() - overall.seen());
        return new ReviewStats(overall, newToday, perSubject);
    }

    private static SubjectStats subjectStats(
            List<ThemeCards> themes, Map<UUID, ReviewState> memory, Instant now, Instant endOfDay) {
        List<ThemeStats> perTheme = themes.stream()
                .map(theme -> new ThemeStats(
                        theme.slug(), theme.title(), CardCounts.of(theme.cardIds(), memory, now, endOfDay)))
                .toList();
        ThemeCards first = themes.getFirst();
        return new SubjectStats(
                first.subjectSlug(),
                first.subjectTitle(),
                CardCounts.of(cardIds(themes), memory, now, endOfDay),
                perTheme);
    }

    private static List<UUID> cardIds(List<ThemeCards> themes) {
        return themes.stream().flatMap(theme -> theme.cardIds().stream()).toList();
    }

    /**
     * @param newToday nouvelles cartes encore proposées aujourd'hui (quota restant, dans la limite du catalogue)
     * @param subjects les sujets suivis, dans l'ordre du catalogue
     */
    public record ReviewStats(CardCounts overall, int newToday, List<SubjectStats> subjects) {}

    /** @param themes les thèmes du sujet, dans l'ordre du document */
    public record SubjectStats(String slug, String title, CardCounts counts, List<ThemeStats> themes) {}

    public record ThemeStats(String slug, String title, CardCounts counts) {}
}
