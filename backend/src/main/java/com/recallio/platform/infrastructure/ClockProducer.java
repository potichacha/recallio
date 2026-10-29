package com.recallio.platform.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Le temps passe toujours par une {@link Clock} injectée (CLAUDE.md, section 4.3) : les tests la remplacent.
 *
 * <p>L'horloge avance à la microseconde, la précision de {@code timestamptz} : un instant relu en base est égal à celui
 * qui a été calculé (sinon une réponse rejouée différerait de la réponse d'origine).
 */
public class ClockProducer {

    private static final Duration DATABASE_PRECISION = Duration.of(1, ChronoUnit.MICROS);

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), DATABASE_PRECISION);
    }
}
