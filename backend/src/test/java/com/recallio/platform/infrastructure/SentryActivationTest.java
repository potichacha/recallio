package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Une seule variable allume Sentry : son DSN (ADR 0070). */
class SentryActivationTest {

    /** Comme dans application.properties : le DSN n'est lu qu'en production. */
    private static final Map<String, String> APPLICATION_PROPERTIES =
            Map.of("%prod.quarkus.log.sentry.dsn", "${SENTRY_DSN:}");

    private static final int APPLICATION_PROPERTIES_ORDINAL = 250;
    private static final int ENVIRONMENT_ORDINAL = 300;

    @Test
    void shouldEnableSentryWhenADsnIsGiven() {
        SmallRyeConfig config = config("prod", Map.of("SENTRY_DSN", "https://public@o1.ingest.de.sentry.io/2"));

        assertThat(sentryEnabled(config)).isTrue();
    }

    @Test
    void shouldLeaveSentryOffWithoutDsn() {
        SmallRyeConfig config = config("prod", Map.of());

        assertThat(sentryEnabled(config)).isFalse();
    }

    @Test
    void shouldLeaveSentryOffWhenTheDsnIsEmpty() {
        SmallRyeConfig config = config("prod", Map.of("SENTRY_DSN", ""));

        assertThat(sentryEnabled(config)).isFalse();
    }

    @Test
    void shouldLeaveSentryOffInDevEvenWithADsn() {
        SmallRyeConfig config = config("dev", Map.of("SENTRY_DSN", "https://public@o1.ingest.de.sentry.io/2"));

        assertThat(sentryEnabled(config)).isFalse();
    }

    @Test
    void shouldLetAnExplicitSettingTurnSentryOffDespiteADsn() {
        SmallRyeConfig config = config(
                "prod",
                Map.of("SENTRY_DSN", "https://public@o1.ingest.de.sentry.io/2", SentryActivation.ENABLED, "false"));

        assertThat(sentryEnabled(config)).isFalse();
    }

    private static SmallRyeConfig config(String profile, Map<String, String> environment) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile(profile)
                .withSources(new PropertiesConfigSource(
                        APPLICATION_PROPERTIES, "application.properties", APPLICATION_PROPERTIES_ORDINAL))
                .withSources(new PropertiesConfigSource(environment, "environment", ENVIRONMENT_ORDINAL))
                .withSources(new SentryActivation())
                .build();
    }

    private static boolean sentryEnabled(SmallRyeConfig config) {
        return config.getOptionalValue(SentryActivation.ENABLED, Boolean.class).orElse(false);
    }
}
