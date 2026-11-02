package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.LiveEventDto;
import com.recallio.live.application.LiveGameEvents;
import com.recallio.live.domain.LiveGame.Snapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import org.jboss.logging.Logger;

/**
 * Diffuse l'état d'une partie à toutes ses connexions WebSocket ({@link LiveGameSocket}), sans attendre leur envoi. Il
 * mesure aussi les connexions ouvertes (ADR 0024) ; créé au démarrage, pour que la jauge existe dès le début.
 */
@Startup
@ApplicationScoped
public class LiveGameBroadcaster implements LiveGameEvents {

    private static final Logger LOG = Logger.getLogger(LiveGameBroadcaster.class);

    private final OpenConnections openConnections;
    private final Clock clock;

    public LiveGameBroadcaster(OpenConnections openConnections, Clock clock, MeterRegistry registry) {
        this.openConnections = openConnections;
        this.clock = clock;
        Gauge.builder("recallio.live.connections", openConnections, LiveGameBroadcaster::liveConnections)
                .description("Connexions WebSocket ouvertes sur des parties live")
                .register(registry);
    }

    private static int liveConnections(OpenConnections connections) {
        return connections.findByEndpointId(LiveGameSocket.ENDPOINT_ID).size();
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
