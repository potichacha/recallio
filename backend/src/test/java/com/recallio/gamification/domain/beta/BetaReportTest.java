package com.recallio.gamification.domain.beta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.gamification.domain.beta.BetaReport.Window;
import com.recallio.gamification.domain.beta.FeatureUsage.FeatureAdoption;
import com.recallio.gamification.domain.beta.TourUsage.ChapterStops;
import com.recallio.gamification.domain.beta.TourUsage.SubjectStops;
import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics.DailyCount;
import com.recallio.platform.domain.UsageStatistics.SubjectCount;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Le rapport de la page « Bêta » (ADR 0076) : la fenêtre, les parts et les taux, sans base ni framework. */
class BetaReportTest {

    private static final Window WINDOW = Window.endingAt(Instant.parse("2026-11-25T10:00:00Z"));

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    @Test
    void shouldMeasureTheThirtyLastDaysOfTheUtcCalendarTodayIncluded() {
        assertThat(WINDOW.first()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(WINDOW.last()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(WINDOW.days())
                .hasSize(BetaReport.DAYS)
                .startsWith(WINDOW.first())
                .endsWith(WINDOW.last());
        assertThat(WINDOW.from()).isEqualTo(Instant.parse("2026-10-27T00:00:00Z"));
        assertThat(WINDOW.to()).isEqualTo(Instant.parse("2026-11-26T00:00:00Z"));
    }

    @Test
    void shouldTakeTodayFromTheUtcCalendarEvenLateInTheEvening() {
        Window lateEvening = Window.endingAt(Instant.parse("2026-11-25T23:59:59Z"));

        assertThat(lateEvening.last()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void shouldRoundAShareDownAndGiveNoneOfNothing() {
        assertThat(BetaReport.percent(2, 3)).isEqualTo(66);
        assertThat(BetaReport.percent(0, 3)).isZero();
        assertThat(BetaReport.percent(3, 3)).isEqualTo(100);
        assertThat(BetaReport.percent(0, 0)).isNull();
    }

    @Test
    void shouldGiveOneRowPerDayOfTheWindowAndZeroWhereNothingHappened() {
        LocalDate today = WINDOW.last();
        LocalDate before = WINDOW.first().minusDays(1);

        List<DayActivity> days = DayActivity.series(
                WINDOW,
                Map.of(
                        DailyCount.REVIEWERS, Map.of(today, 2L),
                        DailyCount.REVIEWS, Map.of(today, 15L, before, 99L),
                        DailyCount.GOALS_REACHED, Map.of(today, 1L),
                        DailyCount.TOUR_VALIDATIONS, Map.of(WINDOW.first(), 4L)));

        assertThat(days).hasSize(BetaReport.DAYS);
        assertThat(days.getFirst()).isEqualTo(new DayActivity(WINDOW.first(), 0, 0, 0, 0, 4));
        assertThat(days.getLast()).isEqualTo(new DayActivity(today, 2, 15, 1, 0, 0));
        assertThat(days).extracting(DayActivity::reviews).doesNotContain(99L);
    }

    @Test
    void shouldRankSubjectsByReviewsThenFollowersAndKeepEveryPublishedOne() {
        List<SubjectActivity> subjects = SubjectActivity.ranked(
                Map.of("java", "Java", "go", "Go", "sql", "SQL"),
                Map.of(
                        SubjectCount.FOLLOWERS, Map.of("java", 3L, "go", 5L, "sql", 1L),
                        SubjectCount.REVIEWERS, Map.of("java", 2L, "cpp", 1L),
                        SubjectCount.REVIEWS, Map.of("java", 40L, "cpp", 7L),
                        SubjectCount.RECALLED, Map.of("java", 30L, "cpp", 7L)));

        assertThat(subjects)
                .extracting(
                        SubjectActivity::slug,
                        SubjectActivity::title,
                        SubjectActivity::followers,
                        SubjectActivity::reviews,
                        SubjectActivity::successPercent)
                .containsExactly(
                        tuple("java", "Java", 3L, 40L, 75),
                        // Révisé sans être publié : il figure, sans titre.
                        tuple("cpp", null, 0L, 7L, 100),
                        // Jamais révisés : le plus suivi d'abord, et aucun taux de réussite.
                        tuple("go", "Go", 5L, 0L, null),
                        tuple("sql", "SQL", 1L, 0L, null));
    }

    @Test
    void shouldCountAsActiveWhoeverUsedADatedFeatureAndGiveEachFeatureItsShare() {
        FeatureUsage usage = FeatureUsage.of(Map.of(
                UsageFeature.REVIEW, Set.of(ALICE, BOB),
                UsageFeature.TOUR, Set.of(CAROL),
                // Bob a un rappel, comme quelqu'un qui n'a rien fait sur la fenêtre : celui-là n'est pas actif.
                UsageFeature.REMINDERS, Set.of(BOB, UUID.randomUUID())));

        assertThat(usage.activeAccounts()).isEqualTo(3);
        assertThat(usage.features()).extracting(FeatureAdoption::feature).containsExactly(UsageFeature.values());
        assertThat(adoption(usage, UsageFeature.REVIEW)).isEqualTo(new FeatureAdoption(UsageFeature.REVIEW, 2, 66));
        assertThat(adoption(usage, UsageFeature.TOUR)).isEqualTo(new FeatureAdoption(UsageFeature.TOUR, 1, 33));
        assertThat(adoption(usage, UsageFeature.REMINDERS))
                .isEqualTo(new FeatureAdoption(UsageFeature.REMINDERS, 1, 33));
        assertThat(adoption(usage, UsageFeature.MOCK_EXAM))
                .isEqualTo(new FeatureAdoption(UsageFeature.MOCK_EXAM, 0, 0));
    }

    @Test
    void shouldGiveNoShareWhenNoAccountIsActive() {
        FeatureUsage usage = FeatureUsage.of(Map.of(UsageFeature.GROUP, Set.of(ALICE)));

        assertThat(usage.activeAccounts()).isZero();
        assertThat(usage.features()).allSatisfy(feature -> {
            assertThat(feature.accounts()).isZero();
            assertThat(feature.percent()).isNull();
        });
    }

    @Test
    void shouldGiveTheFailureRateOfACardRoundedDown() {
        CardLabel label = CardLabel.of(UUID.randomUUID(), null, "java");

        assertThat(new FailedCard(label, 6, 5, 2).failurePercent()).isEqualTo(83);
        assertThatThrownBy(() -> new FailedCard(label, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FailedCard(label, 2, 3, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLabelAnActiveCardWithItsQuestionAndARetiredOneWithItsSubjectOnly() {
        UUID cardId = UUID.randomUUID();
        var snapshot = new CardSnapshot(
                cardId,
                UUID.randomUUID(),
                CardType.TRUE_FALSE,
                "java",
                "Java",
                "java-bases",
                "Les bases",
                "  Une   question\nsur deux lignes. " + "x".repeat(CardLabel.EXCERPT_LENGTH),
                "Oui.",
                List.of(),
                List.of(),
                null,
                "variables",
                "Les variables",
                null);

        CardLabel active = CardLabel.of(cardId, snapshot, "java");
        CardLabel retired = CardLabel.of(cardId, null, "java");

        assertThat(active.subjectTitle()).isEqualTo("Java");
        assertThat(active.themeSlug()).isEqualTo("java-bases");
        assertThat(active.notionTitle()).isEqualTo("Les variables");
        assertThat(active.type()).isEqualTo(CardType.TRUE_FALSE);
        assertThat(active.excerpt())
                .hasSize(CardLabel.EXCERPT_LENGTH)
                .startsWith("Une question sur deux lignes.")
                .endsWith("…");
        assertThat(retired).isEqualTo(new CardLabel(cardId, "java", null, null, null, null, null));
    }

    @Test
    void shouldCountWhereEachAccountStoppedByChapterOfTheTour() {
        NotionInfo first = notion("go", "bases", "debut", "Débuter", 0, 2);
        NotionInfo second = notion("go", "bases", "debut", "Débuter", 1, 1);
        NotionInfo third = notion("go", "bases", "suite", "Poursuivre", 2, 1);
        NotionInfo outOfTour = notion("go", "bases", "fin", "Finir", 3, 0);
        NotionInfo untouched = notion("java", "bases", "debut", "Débuter", 0, 1);
        UUID retired = UUID.randomUUID();

        TourUsage tour = TourUsage.of(
                4,
                List.of(first, second, third, outOfTour, untouched),
                Map.of(first.id(), 1L, second.id(), 2L, retired, 7L),
                Map.of("go", "Go", "java", "Java"));

        assertThat(tour.learners()).isEqualTo(4);
        // Java n'a aucun arrêt : il ne figure pas. Le chapitre sans page du Tour non plus ; celui où personne ne
        // s'est arrêté, si.
        assertThat(tour.stops())
                .containsExactly(new SubjectStops(
                        "go",
                        "Go",
                        List.of(
                                new ChapterStops("Les bases", "Débuter", 3),
                                new ChapterStops("Les bases", "Poursuivre", 0))));
    }

    private static FeatureAdoption adoption(FeatureUsage usage, UsageFeature feature) {
        return usage.features().stream()
                .filter(adoption -> adoption.feature() == feature)
                .findFirst()
                .orElseThrow();
    }

    private static NotionInfo notion(
            String subject, String domain, String chapter, String chapterTitle, int position, int tourPages) {
        return new NotionInfo(
                UUID.randomUUID(),
                subject,
                "notion-" + position,
                "Notion " + position,
                domain,
                "Les bases",
                chapter,
                chapterTitle,
                NotionInfo.Level.BEGINNER,
                1,
                List.of(),
                position,
                3,
                tourPages);
    }
}
