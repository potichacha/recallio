package com.recallio.review.domain.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import com.recallio.review.domain.interview.InterviewCountdown.PlanNotion;
import com.recallio.review.domain.interview.InterviewCountdown.PlannedWeek;
import com.recallio.review.domain.interview.InterviewExceptions.InvalidInterviewDateException;
import com.recallio.review.domain.track.MasteryLevel;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.NotionMastery.StableCards;
import com.recallio.review.domain.track.Readiness;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InterviewCountdownTest {

    /** Un vendredi : la semaine 1 du plan va du vendredi au jeudi suivant, pas du lundi au dimanche. */
    private static final LocalDate PLANNED_ON = LocalDate.of(2026, 11, 20);

    private static final int START = 10;

    /** Dix notions essentielles à acquérir, la ligne de départ de la plupart des tests. */
    private static final List<PlanNotion> TEN = notions(START);

    /** Un indice sous les 80 % : il ne dit pas encore « prêt ». */
    private static final Readiness NOT_READY = new Readiness(START, 0, 0);

    private static final Readiness READY = new Readiness(START, START, START);

    @Test
    void shouldSpreadTheNotionsOverAllWeeksButTheLastOneOfAFourWeekPlan() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON, TEN);

        assertThat(countdown.daysLeft()).isEqualTo(28);
        assertThat(countdown.week()).isEqualTo(1);
        assertThat(countdown.weeks()).isEqualTo(4);
        // ceil(10 × 1 / 3) = 4, puis 7, puis 10 : 4, 3 et 3 notions, et une semaine pour réviser.
        assertThat(countdown.thisWeek().quota()).isEqualTo(4);
        assertThat(countdown.thisWeek().notions()).containsExactlyElementsOf(TEN.subList(0, 4));
        assertThat(countdown.thisWeek().startsOn()).isEqualTo(PLANNED_ON);
        assertThat(countdown.thisWeek().endsOn()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(countdown.thisWeek().review()).isFalse();
        assertThat(countdown.nextWeeks())
                .containsExactly(
                        new PlannedWeek(2, LocalDate.of(2026, 11, 27), LocalDate.of(2026, 12, 3), false, 3),
                        new PlannedWeek(3, LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 10), false, 3),
                        new PlannedWeek(4, LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 17), true, 0));
        assertThat(countdown.status()).isEqualTo(InterviewStatus.ON_TRACK);
        assertThat(countdown.essentialsLeft()).isEqualTo(START);
    }

    @Test
    void shouldAskForEverythingInASingleWeekWithoutAReviewWeek() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(7), PLANNED_ON, TEN);

        assertThat(countdown.weeks()).isEqualTo(1);
        assertThat(countdown.thisWeek().quota()).isEqualTo(START);
        assertThat(countdown.thisWeek().review()).isFalse();
        assertThat(countdown.thisWeek().endsOn()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(countdown.nextWeeks()).isEmpty();
    }

    @Test
    void shouldEndAShortWeekOnTheEveOfTheInterview() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(3), PLANNED_ON, TEN);

        assertThat(countdown.weeks()).isEqualTo(1);
        assertThat(countdown.thisWeek().endsOn()).isEqualTo(LocalDate.of(2026, 11, 22));
    }

    /** Neuf jours, deux semaines entamées : tout la première, la seconde pour réviser. */
    @Test
    void shouldKeepTheSecondWeekOfANineDayPlanForReviewing() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(9), PLANNED_ON, TEN);

        assertThat(countdown.weeks()).isEqualTo(2);
        assertThat(countdown.thisWeek().quota()).isEqualTo(START);
        assertThat(countdown.nextWeeks())
                .containsExactly(new PlannedWeek(2, LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 28), true, 0));
    }

    @Test
    void shouldAskNoNewNotionDuringTheReviewWeekOnceEverythingIsAcquired() {
        LocalDate lastWeek = PLANNED_ON.plusDays(21);

        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), lastWeek, List.of());

        assertThat(countdown.week()).isEqualTo(4);
        assertThat(countdown.thisWeek().review()).isTrue();
        assertThat(countdown.thisWeek().quota()).isZero();
        assertThat(countdown.nextWeeks()).isEmpty();
        // Toutes acquises, mais l'indice n'est pas à 80 % : la maîtrise vient des révisions espacées.
        assertThat(countdown.status()).isEqualTo(InterviewStatus.ON_TRACK);
    }

    @Test
    void shouldAskWhatIsLeftToCatchUpDuringTheReviewWeek() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON.plusDays(21), TEN.subList(8, 10));

        assertThat(countdown.thisWeek().review()).isTrue();
        assertThat(countdown.thisWeek().quota()).isEqualTo(2);
        assertThat(countdown.thisWeek().notions()).containsExactlyElementsOf(TEN.subList(8, 10));
        assertThat(countdown.status()).isEqualTo(InterviewStatus.BEHIND);
    }

    /** Semaine 2 avec 2 notions acquises : l'objectif de la semaine 1 (4) n'est pas tenu, il s'ajoute à celui-ci. */
    @Test
    void shouldAddTheNotionsLeftFromAPastWeekToThisOne() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON.plusDays(7), TEN.subList(2, 10));

        assertThat(countdown.week()).isEqualTo(2);
        assertThat(countdown.thisWeek().quota()).isEqualTo(5);
        assertThat(countdown.thisWeek().notions()).containsExactlyElementsOf(TEN.subList(2, 7));
        assertThat(countdown.status()).isEqualTo(InterviewStatus.BEHIND);
    }

    @Test
    void shouldBeOnTrackWhenThePastWeeksAreDone() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON.plusDays(7), TEN.subList(4, 10));

        assertThat(countdown.thisWeek().quota()).isEqualTo(3);
        assertThat(countdown.status()).isEqualTo(InterviewStatus.ON_TRACK);
    }

    @Test
    void shouldBeAheadOnceTheGoalOfTheWeekIsReachedWithNotionsLeft() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON.plusDays(2), TEN.subList(5, 10));

        assertThat(countdown.thisWeek().quota()).isZero();
        assertThat(countdown.thisWeek().notions()).isEmpty();
        assertThat(countdown.status()).isEqualTo(InterviewStatus.AHEAD);
    }

    /** Une notion qui repasse sous « acquise » (elle reçoit de nouvelles cartes) compte de nouveau parmi les restantes. */
    @Test
    void shouldFallBehindWhenANotionFallsBackBelowAcquired() {
        LocalDate secondWeek = PLANNED_ON.plusDays(7);
        InterviewCountdown onTrack = countdown(PLANNED_ON.plusDays(28), secondWeek, TEN.subList(4, 10));

        InterviewCountdown fallenBack = countdown(PLANNED_ON.plusDays(28), secondWeek, TEN.subList(3, 10));

        assertThat(onTrack.status()).isEqualTo(InterviewStatus.ON_TRACK);
        assertThat(fallenBack.status()).isEqualTo(InterviewStatus.BEHIND);
        assertThat(fallenBack.thisWeek().quota()).isEqualTo(4);
    }

    @Test
    void shouldCountANotionAddedToTheContentAmongTheNotionsLeft() {
        List<PlanNotion> elevenLeft = notions(START + 1);

        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON, elevenLeft);

        assertThat(countdown.essentialsLeft()).isEqualTo(START + 1);
        assertThat(countdown.thisWeek().quota()).isEqualTo(4);
    }

    @Test
    void shouldPlanNothingFromAStartOfZero() {
        var plan = new InterviewPlan("java", PLANNED_ON.plusDays(28), PLANNED_ON, 0);

        InterviewCountdown countdown = InterviewCountdown.of(plan, PLANNED_ON, List.of(), NOT_READY);

        assertThat(countdown.thisWeek().quota()).isZero();
        assertThat(countdown.nextWeeks()).extracting(PlannedWeek::notions).containsOnly(0);
        assertThat(countdown.status()).isEqualTo(InterviewStatus.ON_TRACK);
    }

    @Test
    void shouldCountDownToZeroOnTheDayOfTheInterview() {
        LocalDate interviewOn = PLANNED_ON.plusDays(28);

        InterviewCountdown countdown = countdown(interviewOn, interviewOn, List.of());

        assertThat(countdown.daysLeft()).isZero();
        assertThat(countdown.week()).isEqualTo(4);
        assertThat(countdown.status()).isEqualTo(InterviewStatus.ON_TRACK);
    }

    @Test
    void shouldSayTheInterviewIsPassedBeforeAnythingElse() {
        LocalDate interviewOn = PLANNED_ON.plusDays(28);
        var plan = new InterviewPlan("java", interviewOn, PLANNED_ON, START);

        InterviewCountdown countdown = InterviewCountdown.of(plan, interviewOn.plusDays(1), TEN, READY);

        assertThat(countdown.status()).isEqualTo(InterviewStatus.PASSED);
        assertThat(countdown.daysLeft()).isZero();
        assertThat(countdown.week()).isEqualTo(4);
    }

    @Test
    void shouldSayReadyBeforeBehindOrAhead() {
        var plan = new InterviewPlan("java", PLANNED_ON.plusDays(28), PLANNED_ON, START);

        InterviewCountdown behind = InterviewCountdown.of(plan, PLANNED_ON.plusDays(14), TEN, READY);
        InterviewCountdown ahead = InterviewCountdown.of(plan, PLANNED_ON, TEN.subList(5, 10), READY);

        assertThat(behind.status()).isEqualTo(InterviewStatus.READY);
        assertThat(ahead.status()).isEqualTo(InterviewStatus.READY);
    }

    /** Semaine 3 sans aucune notion acquise : tout le retard s'ajoute à l'objectif de la semaine. */
    @Test
    void shouldCatchUpTwoMissedWeeksInTheThirdOne() {
        InterviewCountdown countdown = countdown(PLANNED_ON.plusDays(28), PLANNED_ON.plusDays(14), TEN);

        assertThat(countdown.status()).isEqualTo(InterviewStatus.BEHIND);
        assertThat(countdown.thisWeek().quota()).isEqualTo(START);
    }

    @Test
    void shouldPlanTheEssentialNotionsNotYetAcquiredInTheOrderOfThePreparationTrack() {
        Map<NotionInfo, NotionMastery> notions = new LinkedHashMap<>();
        NotionInfo third = notion("troisieme", 1, 3);
        NotionInfo first = notion("premiere", 1, 1);
        NotionInfo acquired = notion("acquise", 1, 2);
        NotionInfo common = notion("courante", 2, 0);
        notions.put(third, mastery(MasteryLevel.DISCOVERED));
        notions.put(first, mastery(MasteryLevel.NEW));
        notions.put(acquired, mastery(MasteryLevel.ACQUIRED));
        notions.put(common, mastery(MasteryLevel.NEW));

        assertThat(InterviewCountdown.toAcquire(notions))
                .containsExactly(
                        new PlanNotion("premiere", "premiere", MasteryLevel.NEW),
                        new PlanNotion("troisieme", "troisieme", MasteryLevel.DISCOVERED));
    }

    @Test
    void shouldRefuseADateThatIsNotAfterTodayOrMoreThanAYearAhead() {
        LocalDate today = PLANNED_ON;

        assertThatThrownBy(() -> InterviewPlan.requireValidDate(today, today))
                .isInstanceOf(InvalidInterviewDateException.class);
        assertThatThrownBy(() -> InterviewPlan.requireValidDate(today.minusDays(1), today))
                .isInstanceOf(InvalidInterviewDateException.class);
        assertThatThrownBy(
                        () -> InterviewPlan.requireValidDate(today.plusDays(InterviewPlan.MAX_DAYS_AHEAD + 1), today))
                .isInstanceOf(InvalidInterviewDateException.class);
        InterviewPlan.requireValidDate(today.plusDays(1), today);
        InterviewPlan.requireValidDate(today.plusDays(InterviewPlan.MAX_DAYS_AHEAD), today);
    }

    private static InterviewCountdown countdown(LocalDate interviewOn, LocalDate today, List<PlanNotion> left) {
        var plan = new InterviewPlan("java", interviewOn, PLANNED_ON, START);
        return InterviewCountdown.of(plan, today, left, NOT_READY);
    }

    private static List<PlanNotion> notions(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new PlanNotion("notion-" + index, "Notion " + index, MasteryLevel.NEW))
                .toList();
    }

    /** Une notion de Java, désignée par son slug, qui lui sert aussi de titre. */
    private static NotionInfo notion(String slug, int priority, int position) {
        return new NotionInfo(
                UUID.randomUUID(),
                "java",
                slug,
                slug,
                "domaine",
                "Domaine",
                "chapitre",
                "Chapitre",
                Level.BEGINNER,
                priority,
                List.of(),
                position,
                1,
                0);
    }

    /** Une notion d'une carte, au niveau voulu (NEW, DISCOVERED ou ACQUIRED). */
    private static NotionMastery mastery(MasteryLevel level) {
        Set<CardType> course = Set.of(CardType.FLASHCARD);
        StableCards none = new StableCards(0, Set.of());
        int seen = level == MasteryLevel.NEW ? 0 : 1;
        int recalled = level == MasteryLevel.ACQUIRED ? 1 : 0;
        return new NotionMastery(1, course, seen, recalled, recalled > 0 ? course : Set.of(), none, none);
    }
}
