package com.recallio.review.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Une exécution de code à la fois par utilisateur, exercices et programmes du Tour confondus (ADR 0030, 0037) : un
 * utilisateur n'occupe jamais plus d'une place du runner, qui borne aussi le total. Le verrou est en mémoire, ce qui
 * suffit pour un backend unique (CLAUDE.md, section 4.1).
 */
@ApplicationScoped
public class RunLock {

    private final Set<UUID> running = ConcurrentHashMap.newKeySet();

    /**
     * Exécute {@code run} si l'utilisateur n'a pas d'autre exécution en cours.
     *
     * @param busy l'exception à lever s'il en a une, propre à ce qu'il voulait exécuter
     */
    public <T> T alone(UUID userId, Supplier<T> run, Supplier<? extends RuntimeException> busy) {
        if (!running.add(userId)) {
            throw busy.get();
        }
        try {
            return run.get();
        } finally {
            running.remove(userId);
        }
    }
}
