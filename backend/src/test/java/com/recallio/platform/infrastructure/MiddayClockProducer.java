package com.recallio.platform.infrastructure;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * L'horloge des tests d'intégration ({@code @QuarkusTest}) : elle avance en temps réel, mais part de midi UTC du jour.
 *
 * <p>Avec l'horloge réelle, un test lancé juste avant minuit échouait : une carte notée « À revoir », due une minute
 * plus tard, tombait le lendemain et n'était plus « due aujourd'hui » (CI du 2026-11-02, 23:59:34 UTC). Les tests
 * durent quelques minutes : partir de midi les tient loin de minuit dans tous les fuseaux des tests (CLAUDE.md, section
 * 8). Le temps continue de passer, pour les minuteurs du mode live et des défis.
 */
public class MiddayClockProducer {

    private static final long MIDDAY_HOURS = 12;

    @Produces
    @Alternative
    @Priority(1)
    @ApplicationScoped
    Clock clock() {
        Instant now = Instant.now();
        Instant midday = now.truncatedTo(ChronoUnit.DAYS).plus(MIDDAY_HOURS, ChronoUnit.HOURS);
        return Clock.tick(
                Clock.offset(Clock.systemUTC(), Duration.between(now, midday)), ClockProducer.DATABASE_PRECISION);
    }
}
