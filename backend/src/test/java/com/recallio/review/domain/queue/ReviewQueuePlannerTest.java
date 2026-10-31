package com.recallio.review.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.queue.ReviewQueuePlanner.Candidate;
import com.recallio.review.domain.queue.ReviewQueuePlanner.QueuePlan;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReviewQueuePlannerTest {

    private static final Instant NOW = Instant.parse("2026-10-29T08:00:00Z");
    private static final int LARGE = 100;

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final UUID fourth = UUID.randomUUID();

    @Test
    void shouldPresentDueCardsBeforeNewCards() {
        Map<UUID, ReviewState> states = Map.of(third, dueSince(Duration.ofHours(1)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first, second, third), states, NOW, LARGE, LARGE);

        assertThat(plan.cardIds()).containsExactly(third, first, second);
        assertThat(plan.dueCount()).isEqualTo(1);
        assertThat(plan.newCount()).isEqualTo(2);
    }

    @Test
    void shouldPresentTheMostOverdueCardFirst() {
        Map<UUID, ReviewState> states = Map.of(
                first, dueSince(Duration.ofMinutes(5)),
                second, dueSince(Duration.ofDays(3)),
                third, dueSince(Duration.ofHours(2)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first, second, third), states, NOW, LARGE, LARGE);

        assertThat(plan.cardIds()).containsExactly(second, third, first);
    }

    @Test
    void shouldNotPresentCardsThatAreNotDueYet() {
        Map<UUID, ReviewState> states = Map.of(first, dueIn(Duration.ofDays(2)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first), states, NOW, LARGE, LARGE);

        assertThat(plan.cardIds()).isEmpty();
        assertThat(plan.nextDueAt()).isEqualTo(NOW.plus(Duration.ofDays(2)));
    }

    @Test
    void shouldRespectTheDailyLimitOfNewCards() {
        QueuePlan plan = ReviewQueuePlanner.plan(java(first, second, third, fourth), Map.of(), NOW, 2, LARGE);

        assertThat(plan.cardIds()).containsExactly(first, second);
        assertThat(plan.newCount()).isEqualTo(2);
    }

    @Test
    void shouldStillPresentDueCardsWhenTheNewCardQuotaIsExhausted() {
        Map<UUID, ReviewState> states = Map.of(second, dueSince(Duration.ofHours(1)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first, second), states, NOW, 0, LARGE);

        assertThat(plan.cardIds()).containsExactly(second);
        assertThat(plan.newCount()).isZero();
    }

    @Test
    void shouldLimitTheQueueSizeButReportTheTotals() {
        Map<UUID, ReviewState> states = Map.of(
                first, dueSince(Duration.ofHours(3)),
                second, dueSince(Duration.ofHours(2)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first, second, third, fourth), states, NOW, LARGE, 1);

        assertThat(plan.cardIds()).containsExactly(first);
        assertThat(plan.dueCount()).isEqualTo(2);
        assertThat(plan.newCount()).isEqualTo(2);
    }

    @Test
    void shouldIgnoreStatesOfCardsOutsideTheFilter() {
        UUID otherTheme = UUID.randomUUID();
        Map<UUID, ReviewState> states = Map.of(
                otherTheme, dueSince(Duration.ofDays(1)),
                first, dueIn(Duration.ofMinutes(10)));

        QueuePlan plan = ReviewQueuePlanner.plan(java(first), states, NOW, LARGE, LARGE);

        assertThat(plan.cardIds()).isEmpty();
        assertThat(plan.dueCount()).isZero();
        assertThat(plan.nextDueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void shouldAlternateSubjectsWhenPickingNewCards() {
        UUID go1 = UUID.randomUUID();
        UUID go2 = UUID.randomUUID();
        List<Candidate> eligible = concat(java(first, second, third), subject("go", go1, go2));

        QueuePlan plan = ReviewQueuePlanner.plan(eligible, Map.of(), NOW, 4, LARGE);

        assertThat(plan.cardIds()).containsExactly(first, go1, second, go2);
    }

    @Test
    void shouldGiveTheShareOfAnExhaustedSubjectToTheOthers() {
        UUID go1 = UUID.randomUUID();
        List<Candidate> eligible = concat(java(first, second, third, fourth), subject("go", go1));

        QueuePlan plan = ReviewQueuePlanner.plan(eligible, Map.of(), NOW, 4, LARGE);

        assertThat(plan.cardIds()).containsExactly(first, go1, second, third);
        assertThat(plan.newCount()).isEqualTo(4);
    }

    @Test
    void shouldPresentDueCardsOfAllSubjectsByOverdueness() {
        UUID go1 = UUID.randomUUID();
        Map<UUID, ReviewState> states = Map.of(
                first, dueSince(Duration.ofHours(1)),
                go1, dueSince(Duration.ofDays(1)));

        QueuePlan plan = ReviewQueuePlanner.plan(concat(java(first), subject("go", go1)), states, NOW, 0, LARGE);

        assertThat(plan.cardIds()).containsExactly(go1, first);
    }

    private static List<Candidate> java(UUID... cards) {
        return subject("java", cards);
    }

    private static List<Candidate> subject(String slug, UUID... cards) {
        return Stream.of(cards).map(card -> new Candidate(card, slug)).toList();
    }

    private static List<Candidate> concat(List<Candidate> first, List<Candidate> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private static ReviewState dueSince(Duration overdue) {
        return stateDueAt(NOW.minus(overdue));
    }

    private static ReviewState dueIn(Duration delay) {
        return stateDueAt(NOW.plus(delay));
    }

    private static ReviewState stateDueAt(Instant dueAt) {
        return new ReviewState(LearningState.REVIEW, 0, 3.0, 5.0, dueAt, 2, 0, NOW.minus(Duration.ofDays(3)));
    }
}
