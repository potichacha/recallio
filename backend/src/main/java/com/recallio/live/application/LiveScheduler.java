package com.recallio.live.application;

import java.time.Instant;

/** Réveille une partie à l'échéance de son étape en cours (fin du chrono, fin de la correction). */
public interface LiveScheduler {

    /** Exécute {@code task} à l'instant donné (ou aussitôt s'il est passé). La tâche ne doit pas bloquer. */
    void wakeUpAt(Instant at, Runnable task);
}
