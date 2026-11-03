package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Les tests d'intégration tournent bien sur l'horloge de midi, loin de minuit ({@link MiddayClockProducer}). */
@QuarkusTest
class MiddayClockTest {

    @Inject
    Clock clock;

    @Test
    void shouldRunTheIntegrationTestsAroundMidday() {
        LocalTime now = clock.instant().atOffset(ZoneOffset.UTC).toLocalTime();

        assertThat(now).isBetween(LocalTime.NOON, LocalTime.of(14, 0));
    }
}
