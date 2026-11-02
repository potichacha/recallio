package com.recallio.live.infrastructure;

import com.recallio.live.application.LiveMetrics;
import com.recallio.live.domain.LiveGameRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Les métriques du mode live (ADR 0024) : de quoi voir la charge (parties ouvertes) et l'usage (parties jouées,
 * réponses). Créé au démarrage, pour que la jauge existe avant la première partie.
 */
@Startup
@ApplicationScoped
public class MicrometerLiveMetrics implements LiveMetrics {

    private final Counter started;
    private final Counter finished;
    private final Counter answers;

    public MicrometerLiveMetrics(MeterRegistry registry, LiveGameRepository games) {
        Gauge.builder("recallio.live.games.open", games, LiveGameRepository::openCount)
                .description("Parties live en mémoire (salle d'attente, en cours ou tout juste finies)")
                .register(registry);
        started = Counter.builder("recallio.live.games.started")
                .description("Parties live démarrées")
                .register(registry);
        finished = Counter.builder("recallio.live.games.finished")
                .description("Parties live jouées jusqu'au bout")
                .register(registry);
        answers = Counter.builder("recallio.live.answers")
                .description("Réponses reçues aux questions live")
                .register(registry);
    }

    @Override
    public void gameStarted() {
        started.increment();
    }

    @Override
    public void gameFinished() {
        finished.increment();
    }

    @Override
    public void answerReceived() {
        answers.increment();
    }
}
