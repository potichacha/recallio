package com.recallio.runner.monitoring;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Allume Sentry dès qu'un DSN est donné (ADR 0070), comme dans le backend. L'extension a son interrupteur,
 * {@code quarkus.log.sentry.enabled}, éteint par défaut, et refuse de démarrer s'il est allumé sans DSN : une seule
 * variable, {@code SENTRY_DSN}, décide ainsi des deux. Vide ou absente (en local, en CI, dans le compose), Sentry reste
 * éteint et le runner démarre normalement.
 *
 * <p>Sa source passe après application.properties et les variables d'environnement :
 * {@code QUARKUS_LOG_SENTRY_ENABLED=false} éteint Sentry malgré un DSN. Enregistrée dans
 * {@code META-INF/services/io.smallrye.config.ConfigSourceFactory}.
 */
public class SentryActivation implements ConfigSourceFactory {

    static final String DSN = "quarkus.log.sentry.dsn";
    static final String ENABLED = "quarkus.log.sentry.enabled";

    /** Sous application.properties (250) et les variables d'environnement (300), au-dessus des valeurs par défaut. */
    private static final int ORDINAL = 200;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue dsn = context.getValue(DSN);
        if (dsn == null || dsn.getValue() == null || dsn.getValue().isBlank()) {
            return List.of();
        }
        return List.of(new PropertiesConfigSource(Map.of(ENABLED, "true"), "SentryActivation", ORDINAL));
    }
}
