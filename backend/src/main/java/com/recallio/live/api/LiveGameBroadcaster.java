package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.LiveEventDto;
import com.recallio.live.application.LiveGameEvents;
import com.recallio.live.domain.LiveGame.Snapshot;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import org.jboss.logging.Logger;

/** Diffuse l'état d'une partie à toutes ses connexions WebSocket ({@link LiveGameSocket}), sans attendre leur envoi. */
@ApplicationScoped
public class LiveGameBroadcaster implements LiveGameEvents {

    private static final Logger LOG = Logger.getLogger(LiveGameBroadcaster.class);

    private final OpenConnections openConnections;
    private final Clock clock;

    public LiveGameBroadcaster(OpenConnections openConnections, Clock clock) {
        this.openConnections = openConnections;
        this.clock = clock;
    }

    @Override
    public void changed(Snapshot game) {
        LiveEventDto event = LiveEventDto.game(game, clock.instant());
        String gameId = game.id().toString();
        openConnections.findByEndpointId(LiveGameSocket.ENDPOINT_ID).stream()
                .filter(connection -> connection.isOpen() && gameId.equals(connection.pathParam("gameId")))
                .forEach(connection -> connection
                        .sendText(event)
                        .subscribe()
                        .with(_ -> {}, failure -> LOG.debugf("Diffusion impossible sur %s", connection.id())));
    }
}
