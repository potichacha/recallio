package com.recallio.gamification.domain.funnel;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Les événements du parcours (ADR 0064) : une étape et un instant, sans rien qui désigne une personne. */
public interface FunnelEventRepository {

    void record(UUID id, FunnelStep step, Instant occurredAt);

    /**
     * Le nombre d'événements de chaque étape, de {@code from} inclus à {@code to} exclu ; une étape sans événement est
     * absente.
     */
    Map<FunnelStep, Long> countsBetween(Instant from, Instant to);
}
