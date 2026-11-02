package com.recallio.live.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.live.domain.LiveExceptions.HostOnlyException;
import com.recallio.live.domain.LiveExceptions.LiveGameFullException;
import com.recallio.live.domain.LiveExceptions.LiveGameStartedException;
import com.recallio.live.domain.LiveExceptions.NotInGameException;
import com.recallio.live.domain.LiveExceptions.RoundClosedException;
import com.recallio.live.domain.LiveGame.PlayerView;
import com.recallio.live.domain.LiveGame.Round;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.live.domain.LiveGame.Standing;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LiveGameTest {

    private static final Instant NOW = Instant.parse("2026-11-02T18:00:00Z");

    private final UUID host = UUID.randomUUID();
    private final UUID guest = UUID.randomUUID();
    private final LiveGame game = new LiveGame(UUID.randomUUID(), UUID.randomUUID(), "go", host, NOW);

    @Test
    void shouldListThePlayersInOrderOfArrivalWithTheHostMarked() {
        game.join(host, "Alice", NOW);
        var snapshot = game.join(guest, "Bob", NOW);

        assertThat(snapshot.status()).isEqualTo(LiveGameStatus.LOBBY);
        assertThat(snapshot.players())
                .containsExactly(
                        new PlayerView(host, "Alice", true, false), new PlayerView(guest, "Bob", false, false));
    }

    @Test
    void shouldKeepAPlayerAsLongAsOneOfHisConnectionsIsOpen() {
        game.join(guest, "Bob", NOW);
        game.join(guest, "Bob", NOW);

        assertThat(game.leave(guest, NOW).players()).hasSize(1);
        assertThat(game.leave(guest, NOW).players()).isEmpty();
        assertThat(game.isEmpty()).isTrue();
        assertThat(game.leave(UUID.randomUUID(), NOW).players()).isEmpty();
    }

    @Test
    void shouldRefuseANewPlayerOnceFullButNotAnExtraConnection() {
        for (int index = 0; index < LiveGame.MAX_PLAYERS; index++) {
            game.join(index == 0 ? guest : UUID.randomUUID(), "Joueur", NOW);
        }

        assertThatThrownBy(() -> game.join(UUID.randomUUID(), "Un de trop", NOW))
                .isInstanceOf(LiveGameFullException.class);
        assertThat(game.join(guest, "Joueur", NOW).players()).hasSize(LiveGame.MAX_PLAYERS);
    }

    @Test
    void shouldNoLongerBlockTheGroupWhenNobodyJoinedForFiveMinutes() {
        assertThat(game.isOverAt(NOW.plus(LiveGame.ABANDONED_AFTER).minusSeconds(1)))
                .isFalse();
        assertThat(game.isOverAt(NOW.plus(LiveGame.ABANDONED_AFTER))).isTrue();

        game.join(host, "Alice", NOW);
        assertThat(game.isOverAt(NOW.plusSeconds(3600))).isFalse();
    }

    @Test
    void shouldHandTheHostRoleToTheEarliestPlayerWhenTheHostLeavesTheLobby() {
        UUID third = UUID.randomUUID();
        game.join(host, "Alice", NOW);
        game.join(guest, "Bob", NOW);
        game.join(third, "Carol", NOW);

        var afterLeave = game.leave(host, NOW);

        assertThat(afterLeave.hostId()).isEqualTo(guest);
        assertThat(afterLeave.players()).extracting(PlayerView::host).containsExactly(true, false);
    }

    @Test
    void shouldExpireAnAbandonedLobbyButNotOneWithPlayers() {
        Instant check = game.expiryCheckAt();
        assertThat(check).isEqualTo(NOW.plus(LiveGame.ABANDONED_AFTER));
        assertThat(game.isExpiredAt(check)).isTrue();

        game.join(host, "Alice", NOW);
        assertThat(game.isExpiredAt(check)).isFalse();
    }

    @Nested
    class Rounds {

        private static final LiveTiming TIMING = new LiveTiming(Duration.ofSeconds(20), Duration.ofSeconds(5));

        private final List<LiveQuestion> questions = List.of(question("Un ?"), question("Deux ?"));

        @BeforeEach
        void twoPlayers() {
            game.join(host, "Alice", NOW);
            game.join(guest, "Bob", NOW);
        }

        @Test
        void shouldLetOnlyTheHostStartAndOnlyOnce() {
            assertThatThrownBy(() -> game.start(guest, questions, TIMING, NOW)).isInstanceOf(HostOnlyException.class);

            var started = game.start(host, questions, TIMING, NOW);

            assertThat(started.status()).isEqualTo(LiveGameStatus.QUESTION);
            assertThat(started.round()).isNotNull();
            assertThat(roundOf(started).index()).isZero();
            assertThat(roundOf(started).endsAt()).isEqualTo(NOW.plusSeconds(20));
            assertThatThrownBy(() -> game.start(host, questions, TIMING, NOW))
                    .isInstanceOf(LiveGameStartedException.class);
        }

        @Test
        void shouldRevealAtTheEndOfTheTimerThenAskTheNextQuestionThenFinish() {
            game.start(host, questions, TIMING, NOW);

            assertThat(game.advance(NOW.plusSeconds(19))).isFalse();
            assertThat(game.advance(NOW.plusSeconds(20))).isTrue();
            assertThat(game.snapshot().status()).isEqualTo(LiveGameStatus.REVEAL);
            assertThat(game.phaseEndsAt()).isEqualTo(NOW.plusSeconds(25));

            assertThat(game.advance(NOW.plusSeconds(25))).isTrue();
            assertThat(roundOf(game.snapshot()).index()).isEqualTo(1);
            game.advance(NOW.plusSeconds(45));
            game.advance(NOW.plusSeconds(50));

            assertThat(game.snapshot().status()).isEqualTo(LiveGameStatus.FINISHED);
            assertThat(game.phaseEndsAt()).isNull();
            assertThat(game.isOverAt(NOW.plusSeconds(50))).isTrue();
            assertThat(game.advance(NOW.plusSeconds(3600))).isFalse();
        }

        @Test
        void shouldRevealEarlyOnceEveryPresentPlayerHasAnswered() {
            game.start(host, questions, TIMING, NOW);

            var afterOne = game.answer(host, 0, Set.of(0), NOW.plusSeconds(3));
            assertThat(afterOne.status()).isEqualTo(LiveGameStatus.QUESTION);
            assertThat(afterOne.players()).extracting(PlayerView::answered).containsExactly(true, false);

            // Bob part sans répondre : tous ceux qui restent ont répondu.
            var afterLeave = game.leave(guest, NOW.plusSeconds(4));
            assertThat(afterLeave.status()).isEqualTo(LiveGameStatus.REVEAL);
            assertThat(roundOf(afterLeave).correctCount()).isEqualTo(1);
            assertThat(game.phaseEndsAt()).isEqualTo(NOW.plusSeconds(9));
        }

        @Test
        void shouldKeepTheFirstAnswerAndRefuseLateOrMisplacedOnes() {
            game.start(host, questions, TIMING, NOW);

            game.answer(guest, 0, Set.of(1), NOW.plusSeconds(1));
            game.answer(guest, 0, Set.of(0), NOW.plusSeconds(2));
            assertThatThrownBy(() -> game.answer(host, 1, Set.of(0), NOW.plusSeconds(3)))
                    .isInstanceOf(RoundClosedException.class);
            assertThatThrownBy(() -> game.answer(host, 0, Set.of(0), NOW.plusSeconds(21)))
                    .isInstanceOf(RoundClosedException.class);
            assertThatThrownBy(() -> game.answer(UUID.randomUUID(), 0, Set.of(0), NOW.plusSeconds(3)))
                    .isInstanceOf(NotInGameException.class);

            game.advance(NOW.plusSeconds(20));
            assertThat(roundOf(game.snapshot()).correctCount()).isZero();
        }

        @Test
        void shouldScoreOnlyRevealedQuestionsAndRewardSpeed() {
            game.start(host, questions, TIMING, NOW);
            game.answer(host, 0, Set.of(0), NOW.plusSeconds(5));

            // Pendant le chrono, le classement ne dit rien des réponses déjà données.
            assertThat(game.snapshot().standings()).extracting(Standing::score).containsExactly(0, 0);

            game.answer(guest, 0, Set.of(0), NOW.plusSeconds(10));
            var revealed = game.snapshot().standings();
            assertThat(revealed).extracting(Standing::displayName).containsExactly("Alice", "Bob");
            assertThat(revealed).extracting(Standing::score).containsExactly(875, 750);
            assertThat(revealed).extracting(Standing::lastPoints).containsExactly(875, 750);
            assertThat(revealed).extracting(Standing::rank).containsExactly(1, 2);
        }

        @Test
        void shouldShareTheRankOfExAequoAndKeepThePointsOfAPlayerWhoLeft() {
            game.start(host, questions, TIMING, NOW);
            game.answer(host, 0, Set.of(1), NOW.plusSeconds(1));
            game.answer(guest, 0, Set.of(1), NOW.plusSeconds(2));
            assertThat(game.snapshot().standings()).extracting(Standing::rank).containsExactly(1, 1);

            game.advance(NOW.plusSeconds(7));
            game.answer(guest, 1, Set.of(0), NOW.plusSeconds(7));
            game.leave(guest, NOW.plusSeconds(8));
            game.advance(NOW.plusSeconds(27));
            game.advance(NOW.plusSeconds(32));

            var result = game.result();
            assertThat(result.standings()).extracting(Standing::displayName).containsExactly("Bob", "Alice");
            assertThat(result.standings()).extracting(Standing::correctCount).containsExactly(1, 0);
            assertThat(result.questionCount()).isEqualTo(2);
            assertThat(result.finishedAt()).isEqualTo(NOW.plusSeconds(32));
            assertThat(game.isExpiredAt(
                            NOW.plusSeconds(32).plus(LiveGame.KEPT_AFTER_END).minusSeconds(1)))
                    .isFalse();
            assertThat(game.expiryCheckAt()).isEqualTo(NOW.plusSeconds(32).plus(LiveGame.KEPT_AFTER_END));
            assertThat(game.isExpiredAt(game.expiryCheckAt())).isTrue();
        }

        private static Round roundOf(Snapshot snapshot) {
            return Objects.requireNonNull(snapshot.round(), "aucune question en cours");
        }

        private static LiveQuestion question(String prompt) {
            return new LiveQuestion(UUID.randomUUID(), prompt, List.of("Juste", "Faux"), Set.of(0), "Parce que.");
        }
    }
}
