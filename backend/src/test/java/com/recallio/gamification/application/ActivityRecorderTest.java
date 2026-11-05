package com.recallio.gamification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.content.CardType;
import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.XpReason;
import com.recallio.review.CardReviewed;
import com.recallio.user.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityRecorderTest {

    private static final int DAILY_GOAL = 2;

    private final InMemoryActivity activity = new InMemoryActivity();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final ActivityRecorder recorder =
            new ActivityRecorder(activity, ledger, new TestConfig(), UUID::randomUUID);

    @Test
    void shouldCountTheReviewOnTheDayOfTheUserTimezone() {
        // 23 h 30 UTC le 28 octobre = 00 h 30 le 29 octobre à Paris.
        var paris = new UserRef(UUID.randomUUID(), ZoneId.of("Europe/Paris"));

        recorder.onCardReviewed(reviewed(paris, "2026-10-28T23:30:00Z"));

        assertThat(activity.cards).containsOnlyKeys(LocalDate.of(2026, 10, 29));
    }

    @Test
    void shouldCountTheRightDayAroundADaylightSavingChange() {
        // Passage à l'heure d'hiver à Paris le 25 octobre 2026 à 01 h 00 UTC : la journée dure 25 heures,
        // de 22 h 00 UTC le 24 à 23 h 00 UTC le 25.
        var paris = new UserRef(UUID.randomUUID(), ZoneId.of("Europe/Paris"));

        recorder.onCardReviewed(reviewed(paris, "2026-10-24T22:30:00Z")); // 00 h 30 (heure d'été)
        recorder.onCardReviewed(reviewed(paris, "2026-10-25T01:30:00Z")); // 02 h 30 (heure d'hiver)
        recorder.onCardReviewed(reviewed(paris, "2026-10-25T22:59:00Z")); // 23 h 59
        recorder.onCardReviewed(reviewed(paris, "2026-10-25T23:00:00Z")); // minuit, le 26

        assertThat(activity.cards)
                .containsOnly(Map.entry(LocalDate.of(2026, 10, 25), 3), Map.entry(LocalDate.of(2026, 10, 26), 1));
    }

    @Test
    void shouldAwardXpToTheSubjectOfTheCardAndTheDailyBonusToNoSubject() {
        var user = new UserRef(UUID.randomUUID(), ZoneId.of("UTC"));

        recorder.onCardReviewed(reviewed(user, "2026-10-29T08:00:00Z"));
        recorder.onCardReviewed(reviewed(user, "2026-10-29T08:01:00Z"));
        recorder.onCardReviewed(reviewed(user, "2026-10-29T08:02:00Z"));

        assertThat(ledger.entries)
                .extracting(XpLedger.XpEntry::reason, XpLedger.XpEntry::subjectSlug)
                .containsExactly(
                        tuple(XpReason.REVIEW, "java"),
                        tuple(XpReason.REVIEW, "java"),
                        tuple(XpReason.DAILY_GOAL, null),
                        tuple(XpReason.REVIEW, "java"));
        assertThat(ledger.totals(user.id())).isEqualTo(new XpLedger.XpTotals(10 + 10 + 50 + 10, Map.of("java", 30L)));
    }

    @Test
    void shouldAwardTheBonusOfASuccessGradedByTheServer() {
        var user = new UserRef(UUID.randomUUID(), ZoneId.of("UTC"));
        UUID reviewId = UUID.randomUUID();

        recorder.onCardReviewed(new CardReviewed(
                user,
                reviewId,
                UUID.randomUUID(),
                "go",
                CardType.CODING_EXERCISE,
                true,
                Instant.parse("2026-10-29T08:00:00Z")));

        assertThat(ledger.entries)
                .extracting(XpLedger.XpEntry::reason, XpLedger.XpEntry::amount, XpLedger.XpEntry::sourceId)
                .containsExactly(tuple(XpReason.REVIEW, 40, reviewId), tuple(XpReason.VERIFIED_SUCCESS, 20, reviewId));
    }

    private static CardReviewed reviewed(UserRef user, String at) {
        return new CardReviewed(
                user, UUID.randomUUID(), UUID.randomUUID(), "java", CardType.FLASHCARD, true, Instant.parse(at));
    }

    private record TestConfig() implements GamificationConfig {
        @Override
        public int xpPerReview() {
            return 10;
        }

        @Override
        public int dailyGoal() {
            return DAILY_GOAL;
        }

        @Override
        public int dailyGoalBonus() {
            return 50;
        }
    }

    private static final class InMemoryActivity implements ActivityRepository {

        final Map<LocalDate, Integer> cards = new HashMap<>();

        @Override
        public int recordReview(UUID userId, LocalDate day, int goal) {
            return cards.merge(day, 1, Integer::sum);
        }

        @Override
        public int cardsReviewed(UUID userId, LocalDate day) {
            return cards.getOrDefault(day, 0);
        }

        @Override
        public long totalReviews(UUID userId) {
            return cards.values().stream().mapToLong(Integer::longValue).sum();
        }

        @Override
        public List<DailyActivity> between(UUID userId, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<LocalDate> goalDays(UUID userId) {
            return cards.entrySet().stream()
                    .filter(entry -> entry.getValue() >= DAILY_GOAL)
                    .map(Map.Entry::getKey)
                    .toList();
        }

        @Override
        public java.util.Map<UUID, java.util.Set<LocalDate>> activeDays() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryLedger implements XpLedger {

        final List<XpEntry> entries = new ArrayList<>();

        @Override
        public void append(XpEntry entry) {
            entries.add(entry);
        }

        @Override
        public XpTotals totals(UUID userId) {
            long total = 0;
            Map<String, Long> bySubject = new HashMap<>();
            for (XpEntry entry : entries) {
                total += entry.amount();
                String subjectSlug = entry.subjectSlug();
                if (subjectSlug != null) {
                    bySubject.merge(subjectSlug, (long) entry.amount(), Long::sum);
                }
            }
            return new XpTotals(total, bySubject);
        }
    }
}
