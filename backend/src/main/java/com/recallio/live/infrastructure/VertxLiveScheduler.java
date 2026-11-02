package com.recallio.live.infrastructure;

import com.recallio.live.application.LiveScheduler;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Les réveils des parties, par des minuteurs Vert.x : ils ne bloquent aucun thread pendant l'attente. La tâche s'exécute
 * sur la boucle d'événements, d'où l'exigence qu'elle ne bloque pas.
 */
@ApplicationScoped
public class VertxLiveScheduler implements LiveScheduler {

    /** Vert.x refuse un délai nul ; une milliseconde de plus garantit aussi que l'échéance est bien passée au réveil. */
    private static final long MIN_DELAY_MS = 1;

    private static final Logger LOG = Logger.getLogger(VertxLiveScheduler.class);

    private final Vertx vertx;
    private final Clock clock;

    public VertxLiveScheduler(Vertx vertx, Clock clock) {
        this.vertx = vertx;
        this.clock = clock;
    }

    @Override
    public void wakeUpAt(Instant at, Runnable task) {
        long delay =
                Math.max(MIN_DELAY_MS, Duration.between(clock.instant(), at).toMillis() + MIN_DELAY_MS);
        vertx.setTimer(delay, _ -> task.run());
    }

    @Override
    public void runBlocking(Runnable task) {
        vertx.executeBlocking(() -> {
                    task.run();
                    return null;
                })
                .onFailure(failure -> LOG.error("Tâche du mode live en échec", failure));
    }
}
