package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.gamification.domain.ScoringRules.PointsAward;
import org.junit.jupiter.api.Test;

class ScoringRulesTest {

    private final ScoringRules rules = new ScoringRules(10, 3, 50);

    @Test
    void shouldAwardTheSamePointsForEveryReview() {
        assertThat(rules.awardsFor(1)).containsExactly(new PointsAward(PointsReason.REVIEW, 10));
    }

    @Test
    void shouldAwardTheBonusWhenTheDailyGoalIsReached() {
        assertThat(rules.awardsFor(3))
                .containsExactly(
                        new PointsAward(PointsReason.REVIEW, 10), new PointsAward(PointsReason.DAILY_GOAL, 50));
    }

    @Test
    void shouldAwardTheBonusOnlyOncePerDay() {
        assertThat(rules.awardsFor(4)).containsExactly(new PointsAward(PointsReason.REVIEW, 10));
    }

    @Test
    void shouldRejectAGoalOfZeroCards() {
        assertThatThrownBy(() -> new ScoringRules(10, 0, 50)).isInstanceOf(IllegalArgumentException.class);
    }
}
