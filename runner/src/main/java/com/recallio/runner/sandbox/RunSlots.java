package com.recallio.runner.sandbox;

import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Les exécutions simultanées, soumissions et programmes confondus (ADR 0030, 0037) : chacune occupe un conteneur, et la
 * machine n'en supporte qu'un nombre borné.
 */
@Singleton
public class RunSlots {

    private final Semaphore slots;

    RunSlots(SandboxConfig config) {
        this.slots = new Semaphore(config.maxConcurrentRuns());
    }

    /** Exécute {@code run} s'il reste une place ; vide sinon, sans attendre. */
    public <T> Optional<T> tryRun(Supplier<T> run) {
        if (!slots.tryAcquire()) {
            return Optional.empty();
        }
        try {
            return Optional.of(run.get());
        } finally {
            slots.release();
        }
    }
}
