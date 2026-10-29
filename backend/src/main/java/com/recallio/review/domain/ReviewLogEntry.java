package com.recallio.review.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Une ligne du journal de révision, jamais modifiée (CLAUDE.md, section 6.2) : il permettra de réentraîner les poids
 * FSRS et de produire des statistiques. L'état obtenu après la révision y est recopié.
 *
 * @param requestId identifiant choisi par le client : rejouer la même requête ne crée pas de seconde révision
 * @param elapsedMs temps de réflexion mesuré par le client, s'il l'a fourni
 */
public record ReviewLogEntry(
        UUID id,
        UUID requestId,
        UUID userId,
        UUID cardId,
        UUID cardVersionId,
        Rating rating,
        Instant reviewedAt,
        @Nullable Integer elapsedMs,
        LearningState state,
        double stability,
        double difficulty,
        Instant dueAt) {}
