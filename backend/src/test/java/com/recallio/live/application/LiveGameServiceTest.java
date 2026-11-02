package com.recallio.live.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardDirectory;
import com.recallio.group.GroupReviewScopes.GroupReviewScope;
import com.recallio.live.domain.LiveGame;
import com.recallio.live.domain.LiveGameStatus;
import com.recallio.live.domain.LiveQuestion;
import com.recallio.live.infrastructure.InMemoryLiveGameRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Les réveils d'une partie : le minuteur ne doit jamais la laisser bloquée sur une étape. */
class LiveGameServiceTest {

    private static final Instant START = Instant.parse("2026-11-02T18:00:00Z");
    private static final LiveConfig CONFIG = new TestConfig();

    private final MutableClock clock = new MutableClock(START);
    private final List<Wake> wakes = new ArrayList<>();
    private final InMemoryLiveGameRepository games = new InMemoryLiveGameRepository();
    private final LiveGameService service = new LiveGameService(
            games,
            new LiveQuestionDraw(new NoCards(), CONFIG),
            _ -> {},
            (at, task) -> wakes.add(new Wake(at, task)),
            (_, _) -> {},
            (_, _) -> new GroupReviewScope(Set.of("go"), Set.of(), Set.of()),
            _ -> Map.of(),
            UUID::randomUUID,
            CONFIG,
            clock);

    private final UUID host = UUID.randomUUID();
    private final LiveGame game = new LiveGame(UUID.randomUUID(), UUID.randomUUID(), "go", host, START);

    @Test
    void shouldRescheduleAWakeUpThatComesSlightlyBeforeTheDeadline() {
        startGame();
        Instant deadline = START.plus(CONFIG.timing().question());

        // Le minuteur se réveille une milliseconde trop tôt pour l'horloge : rien ne change, il se reprogramme.
        clock.set(deadline.minusMillis(1));
        service.wakeUp(game.id(), deadline);
        assertThat(game.snapshot().status()).isEqualTo(LiveGameStatus.QUESTION);
        assertThat(wakes).extracting(Wake::at).containsExactly(deadline);

        clock.set(deadline);
        wakes.removeFirst().task().run();
        assertThat(game.snapshot().status()).isEqualTo(LiveGameStatus.REVEAL);
        assertThat(wakes)
                .extracting(Wake::at)
                .containsExactly(deadline.plus(CONFIG.timing().reveal()));
    }

    @Test
    void shouldIgnoreAWakeUpForADeadlineThatNoLongerHolds() {
        startGame();
        Instant questionDeadline = START.plus(CONFIG.timing().question());
        // Le seul joueur répond : la correction est anticipée, l'échéance de la question ne tient plus.
        clock.set(START.plusSeconds(1));
        game.answer(host, 0, Set.of(0), clock.instant());

        clock.set(questionDeadline);
        service.wakeUp(game.id(), questionDeadline);

        assertThat(wakes).isEmpty();
    }

    private void startGame() {
        game.join(host, "Alice", START);
        game.start(
                host,
                List.of(new LiveQuestion(UUID.randomUUID(), "Un ?", List.of("Oui", "Non"), Set.of(0), "Parce que.")),
                CONFIG.timing(),
                START);
        games.registerUnlessGroupHasOne(game);
    }

    private record Wake(Instant at, Runnable task) {}

    private record TestConfig() implements LiveConfig {
        @Override
        public int questionsPerGame() {
            return 1;
        }

        @Override
        public int minQuestions() {
            return 1;
        }

        @Override
        public int questionSeconds() {
            return 20;
        }

        @Override
        public int revealSeconds() {
            return 5;
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Ces tests ne tirent pas de questions : le catalogue n'est jamais lu. */
    private static final class NoCards implements CardDirectory {
        @Override
        public List<ActiveCard> activeCards(CardFilter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CardSnapshot> currentVersions(Collection<UUID> cardIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ThemeCards> activeCardsByTheme() {
            throw new UnsupportedOperationException();
        }
    }
}
