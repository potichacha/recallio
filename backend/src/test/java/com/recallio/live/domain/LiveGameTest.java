package com.recallio.live.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.live.domain.LiveExceptions.LiveGameFullException;
import com.recallio.live.domain.LiveGame.PlayerView;
import java.time.Instant;
import java.util.UUID;
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
                .containsExactly(new PlayerView(host, "Alice", true), new PlayerView(guest, "Bob", false));
    }

    @Test
    void shouldKeepAPlayerAsLongAsOneOfHisConnectionsIsOpen() {
        game.join(guest, "Bob", NOW);
        game.join(guest, "Bob", NOW);

        assertThat(game.leave(guest).players()).hasSize(1);
        assertThat(game.leave(guest).players()).isEmpty();
        assertThat(game.isEmpty()).isTrue();
        assertThat(game.leave(UUID.randomUUID()).players()).isEmpty();
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
    void shouldBeAbandonedWhenNobodyJoinedForFiveMinutes() {
        assertThat(game.isAbandonedAt(NOW.plus(LiveGame.ABANDONED_AFTER).minusSeconds(1)))
                .isFalse();
        assertThat(game.isAbandonedAt(NOW.plus(LiveGame.ABANDONED_AFTER))).isTrue();

        game.join(host, "Alice", NOW);
        assertThat(game.isAbandonedAt(NOW.plusSeconds(3600))).isFalse();
    }
}
