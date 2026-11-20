package com.recallio.review.domain.daily;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DailyChallengeAttemptTest {

    private static final List<Integer> FIVE_QUESTIONS = List.of(0, 1, 2, 3, 4);

    @Test
    void shouldBeStartedAtTheFirstAnswerAndDoneOnceEveryQuestionHasOne() {
        var none = new DailyChallengeAttempt(FIVE_QUESTIONS, Map.of());
        var some = new DailyChallengeAttempt(FIVE_QUESTIONS, Map.of(0, true, 1, false));
        var all = new DailyChallengeAttempt(FIVE_QUESTIONS, Map.of(0, true, 1, false, 2, true, 3, true, 4, false));

        assertThat(none.state()).isEqualTo(DailyChallengeState.NOT_STARTED);
        assertThat(none.isStarted()).isFalse();
        assertThat(some.state()).isEqualTo(DailyChallengeState.IN_PROGRESS);
        assertThat(some.isStarted()).isTrue();
        assertThat(all.state()).isEqualTo(DailyChallengeState.DONE);
        assertThat(all.score()).isEqualTo(3);
        assertThat(all.total()).isEqualTo(5);
    }

    @Test
    void shouldFinishWithoutAQuestionWhoseCardWasRetired() {
        // La carte de la question 2 a été retirée depuis le tirage : le défi se finit et se compte sur les quatre
        // autres.
        var attempt = new DailyChallengeAttempt(List.of(0, 1, 3, 4), Map.of(0, true, 1, true, 3, true, 4, true));

        assertThat(attempt.state()).isEqualTo(DailyChallengeState.DONE);
        assertThat(attempt.score()).isEqualTo(4);
        assertThat(attempt.total()).isEqualTo(4);
    }

    @Test
    void shouldCountThePlayersOfEachScoreAndTheirAverageToOneDecimal() {
        DailyChallengeStats stats = DailyChallengeStats.of(Map.of(5, 1, 3, 2), 5);

        assertThat(stats.players()).isEqualTo(3);
        // (5 + 3 + 3) / 3 = 3,666…
        assertThat(stats.averageScore()).isEqualTo(3.7);
        assertThat(stats.playersByScore()).containsExactly(0, 0, 0, 2, 0, 1);
    }

    @Test
    void shouldHaveNoAverageWithoutPlayers() {
        DailyChallengeStats stats = DailyChallengeStats.of(Map.of(), 5);

        assertThat(stats.players()).isZero();
        assertThat(stats.averageScore()).isZero();
        assertThat(stats.playersByScore()).hasSize(6).containsOnly(0);
    }
}
