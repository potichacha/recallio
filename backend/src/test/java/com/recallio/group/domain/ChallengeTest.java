package com.recallio.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.group.domain.ChallengeRun.Attempt;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ChallengeTest {

    private static final Instant NOW = Instant.parse("2026-11-02T08:00:00Z");

    private final Challenge challenge = challengeOf(3, 20);

    @Test
    void shouldStayOpenForAWeek() {
        assertThat(challenge.endsAt()).isEqualTo(Instant.parse("2026-11-09T08:00:00Z"));
        assertThat(challenge.isOpenAt(NOW.plus(Duration.ofDays(6)))).isTrue();
        assertThat(challenge.isOpenAt(challenge.endsAt())).isFalse();
    }

    @Test
    void shouldAcceptAnAnswerUntilTheDeadlinePlusTheNetworkGrace() {
        assertThat(challenge.deadline(NOW)).isEqualTo(NOW.plusSeconds(20));
        assertThat(challenge.answeredInTime(NOW, NOW.plusSeconds(22))).isTrue();
        assertThat(challenge.answeredInTime(NOW, NOW.plusSeconds(22).plusMillis(1)))
                .isFalse();
    }

    @Test
    void shouldBoundTheTitleTheSeriesAndTheDelay() {
        assertThatThrownBy(() -> challengeOf(Challenge.MIN_CARDS - 1, 20)).isInstanceOf(InvalidGroupException.class);
        assertThatThrownBy(() -> challengeOf(Challenge.MAX_CARDS + 1, 20)).isInstanceOf(InvalidGroupException.class);
        assertThatThrownBy(() -> challengeOf(3, Challenge.MIN_SECONDS - 1)).isInstanceOf(InvalidGroupException.class);
        assertThatThrownBy(
                        () -> Challenge.create(UUID.randomUUID(), UUID.randomUUID(), "  ", cards(3), 20, owner(), NOW))
                .isInstanceOf(InvalidGroupException.class);
    }

    @Test
    void shouldFollowARunCardAfterCard() {
        var answered = new Attempt(0, NOW, NOW.plusSeconds(4), true, false);
        var late = new Attempt(1, NOW.plusSeconds(10), NOW.plusSeconds(30), false, true);
        var pending = new Attempt(2, NOW.plusSeconds(40), null, null, false);

        ChallengeRun inProgress = new ChallengeRun(challenge, List.of(pending, answered, late));
        assertThat(inProgress.pending()).contains(pending);
        assertThat(inProgress.isFinished()).isFalse();
        assertThat(inProgress.nextPosition()).isEqualTo(3);

        var lastAnswered = new Attempt(2, NOW.plusSeconds(40), NOW.plusSeconds(43), true, false);
        ChallengeRun finished = new ChallengeRun(challenge, List.of(answered, late, lastAnswered));
        assertThat(finished.isFinished()).isTrue();
        assertThat(finished.score()).isEqualTo(2);
        assertThat(finished.time()).isEqualTo(Duration.ofSeconds(4 + 20 + 3));
        assertThat(new ChallengeRun(challenge, List.of()).isStarted()).isFalse();
    }

    private static Challenge challengeOf(int cardCount, int seconds) {
        return Challenge.create(
                UUID.randomUUID(), UUID.randomUUID(), " Défi ", cards(cardCount), seconds, owner(), NOW);
    }

    private static List<UUID> cards(int count) {
        return IntStream.range(0, count).mapToObj(_ -> UUID.randomUUID()).toList();
    }

    private static UUID owner() {
        return UUID.randomUUID();
    }
}
