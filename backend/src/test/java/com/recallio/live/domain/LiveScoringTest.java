package com.recallio.live.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LiveScoringTest {

    private static final Duration CHRONO = Duration.ofSeconds(20);

    @Test
    void shouldRewardARightAnswerMoreTheFasterItCame() {
        assertThat(LiveScoring.points(true, Duration.ZERO, CHRONO)).isEqualTo(1000);
        assertThat(LiveScoring.points(true, Duration.ofSeconds(5), CHRONO)).isEqualTo(875);
        assertThat(LiveScoring.points(true, Duration.ofSeconds(10), CHRONO)).isEqualTo(750);
        assertThat(LiveScoring.points(true, CHRONO, CHRONO)).isEqualTo(LiveScoring.CORRECT);
    }

    @Test
    void shouldGiveNothingForAWrongAnswerHoweverFast() {
        assertThat(LiveScoring.points(false, Duration.ZERO, CHRONO)).isZero();
    }

    @Test
    void shouldNeverGoBelowTheBaseForARightAnswer() {
        assertThat(LiveScoring.points(true, Duration.ofSeconds(25), CHRONO)).isEqualTo(LiveScoring.CORRECT);
    }
}
