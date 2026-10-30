package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ThemeCards;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.stats.CardCounts;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cas d'usage : le tableau de bord de révision (cartes dues, progression par thème). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ReviewStatsService {

    private final CardDirectory cards;
    private final ReviewStateRepository states;
    private final NewCardQuota quota;
    private final Clock clock;

    public ReviewStatsService(CardDirectory cards, ReviewStateRepository states, NewCardQuota quota, Clock clock) {
        this.cards = cards;
        this.states = states;
        this.quota = quota;
        this.clock = clock;
    }

    public ReviewStats stats(UserRef user) {
        Instant now = clock.instant();
        Instant endOfDay = NewCardQuota.startOfNextDay(now, user.timezone());
        Map<UUID, ReviewState> memory = states.findAllByUser(user.id());
        List<ThemeCards> themes = cards.activeCardsByTheme();

        List<ThemeStats> perTheme = themes.stream()
                .map(theme -> new ThemeStats(
                        theme.subjectSlug(),
                        theme.subjectTitle(),
                        theme.slug(),
                        theme.title(),
                        CardCounts.of(theme.cardIds(), memory, now, endOfDay)))
                .toList();
        List<UUID> allCards =
                themes.stream().flatMap(theme -> theme.cardIds().stream()).toList();
        CardCounts overall = CardCounts.of(allCards, memory, now, endOfDay);
        int newToday = Math.min(quota.remainingToday(user, now), overall.total() - overall.seen());
        return new ReviewStats(overall, newToday, perTheme);
    }

    /** @param newToday nouvelles cartes encore proposées aujourd'hui (quota restant, dans la limite du catalogue) */
    public record ReviewStats(CardCounts overall, int newToday, List<ThemeStats> themes) {}

    public record ThemeStats(String subjectSlug, String subjectTitle, String slug, String title, CardCounts counts) {}
}
