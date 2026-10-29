package com.recallio.review.domain.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewState;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardCountsTest {

    private static final Instant NOW = Instant.parse("2026-10-29T08:00:00Z");
    private static final Instant END_OF_DAY = Instant.parse("2026-10-29T23:00:00Z");

    private final UUID fresh = UUID.randomUUID();
    private final UUID learning = UUID.randomUUID();
    private final UUID dueTonight = UUID.randomUUID();
    private final UUID mastered = UUID.randomUUID();

    @Test
    void shouldCountSeenMasteredAndDueCards() {
        Map<UUID, ReviewState> states = Map.of(
                learning, state(LearningState.LEARNING, 1, NOW.minus(Duration.ofMinutes(5))),
                dueTonight, state(LearningState.REVIEW, 4, NOW.plus(Duration.ofHours(10))),
                mastered, state(LearningState.REVIEW, 40, NOW.plus(Duration.ofDays(30))));

        CardCounts counts = CardCounts.of(List.of(fresh, learning, dueTonight, mastered), states, NOW, END_OF_DAY);

        assertThat(counts).isEqualTo(new CardCounts(4, 3, 1, 1, 2));
    }

    @Test
    void shouldNotCountACardInRelearningAsMastered() {
        Map<UUID, ReviewState> states = Map.of(mastered, state(LearningState.RELEARNING, 30, NOW.plusSeconds(600)));

        assertThat(CardCounts.of(List.of(mastered), states, NOW, END_OF_DAY).mastered())
                .isZero();
    }

    @Test
    void shouldIgnoreStatesOfCardsOutsideTheSet() {
        Map<UUID, ReviewState> states = Map.of(learning, state(LearningState.LEARNING, 1, NOW));

        assertThat(CardCounts.of(List.of(fresh), states, NOW, END_OF_DAY)).isEqualTo(new CardCounts(1, 0, 0, 0, 0));
    }

    private static ReviewState state(LearningState state, double stability, Instant dueAt) {
        return new ReviewState(state, 0, stability, 5, dueAt, 3, 0, NOW.minus(Duration.ofDays(1)));
    }
}
