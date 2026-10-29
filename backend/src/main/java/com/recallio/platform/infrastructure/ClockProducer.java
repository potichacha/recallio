package com.recallio.platform.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/** Le temps passe toujours par une {@link Clock} injectée (CLAUDE.md, section 4.3) : les tests la remplacent. */
public class ClockProducer {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.systemUTC();
    }
}
