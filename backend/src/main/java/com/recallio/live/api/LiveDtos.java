package com.recallio.live.api;

import com.recallio.live.domain.LiveGame.PlayerView;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.live.domain.LiveGameStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Contrat JSON du mode live, en REST comme sur le WebSocket (ADR 0021). */
final class LiveDtos {

    private LiveDtos() {}

    @Schema(name = "OpenLiveGameRequest")
    record OpenLiveGameRequest(
            @Schema(description = "Slug d'un des sujets du groupe") @NotBlank
            String subject) {}

    @Schema(name = "LiveGame")
    record LiveGameDto(
            UUID id,
            UUID groupId,
            String subject,
            UUID hostId,
            Instant createdAt,
            LiveGameStatus status,

            @Schema(description = "Les joueurs connectés, dans l'ordre d'arrivée")
            List<LivePlayerDto> players) {
        static LiveGameDto from(Snapshot game) {
            return new LiveGameDto(
                    game.id(),
                    game.groupId(),
                    game.subjectSlug(),
                    game.hostId(),
                    game.createdAt(),
                    game.status(),
                    game.players().stream().map(LivePlayerDto::from).toList());
        }
    }

    @Schema(name = "LivePlayer")
    record LivePlayerDto(UUID userId, String displayName, boolean host) {
        static LivePlayerDto from(PlayerView player) {
            return new LivePlayerDto(player.userId(), player.displayName(), player.host());
        }
    }

    /**
     * Un message du serveur sur le WebSocket. {@code type} dit comment le lire ; cette étape n'envoie que l'état de la
     * partie ({@code game}), les suivantes ajoutent les questions et les résultats.
     */
    record LiveEventDto(String type, LiveGameDto game) {
        static final String GAME = "game";

        static LiveEventDto game(Snapshot game) {
            return new LiveEventDto(GAME, LiveGameDto.from(game));
        }
    }
}
