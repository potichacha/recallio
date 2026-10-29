package com.recallio.gamification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Registre des points, append-only (CLAUDE.md, section 6.3) : le total est la somme des entrées, jamais un compteur
 * modifié en place. Il permet de recalculer n'importe quel total et d'expliquer chaque point.
 */
public interface PointsLedger {

    /**
     * Ajoute l'entrée, sauf si une entrée existe déjà pour le même utilisateur, la même raison et la même source.
     */
    void append(PointsEntry entry);

    long total(UUID userId);

    /**
     * @param sourceId ce qui a rapporté les points : la révision, ou celle qui a permis d'atteindre l'objectif
     */
    record PointsEntry(UUID id, UUID userId, int amount, PointsReason reason, UUID sourceId, Instant occurredAt) {}
}
