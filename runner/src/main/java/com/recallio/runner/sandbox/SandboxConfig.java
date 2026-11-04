package com.recallio.runner.sandbox;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.Duration;
import java.util.Optional;

/** Réglages du bac à sable (ADR 0030), surchargeables par variables d'environnement ({@code RECALLIO_SANDBOX_...}). */
@ConfigMapping(prefix = "recallio.sandbox")
public interface SandboxConfig {

    /** L'image du bac à sable Java ({@code sandbox/java}). */
    @WithDefault("recallio-sandbox-java")
    String javaImage();

    /** L'image du bac à sable Go ({@code sandbox/go}, ADR 0033). */
    @WithDefault("recallio-sandbox-go")
    String goImage();

    /** Le délai d'une exécution, compilation comprise ; au-delà, le conteneur est tué. */
    @WithDefault("20s")
    Duration timeout();

    /** Exécutions simultanées au plus : au-delà, le runner répond 503 plutôt que d'épuiser la machine. */
    @WithDefault("4")
    int maxConcurrentRuns();

    /** Un runtime de conteneur plus isolant que runc, s'il est installé : {@code runsc} pour gVisor. */
    Optional<String> dockerRuntime();

    /** L'image d'un langage. */
    default String image(Language language) {
        return switch (language) {
            case JAVA -> javaImage();
            case GO -> goImage();
        };
    }
}
