package com.recallio.live.application;

import java.time.Instant;

/**
 * Réveille une partie à l'échéance de son étape en cours (fin du chrono, fin de la correction), et met à l'écart ce qui
 * bloque (la base de données) : les réveils s'exécutent sur la boucle d'événements.
 */
public interface LiveScheduler {

    /** Exécute {@code task} à l'instant donné (ou aussitôt s'il est passé). La tâche ne doit pas bloquer. */
    void wakeUpAt(Instant at, Runnable task);

    /** Exécute {@code task}, qui peut bloquer, sur un thread de travail ; un échec est journalisé. */
    void runBlocking(Runnable task);
}
