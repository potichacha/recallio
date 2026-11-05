package com.recallio.review.domain.tour;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Les notions du Tour qu'un utilisateur a validées (ADR 0037). */
public interface TourProgressRepository {

    /** Garde la première validation : valider de nouveau ne change rien. */
    void markValidated(UUID userId, UUID notionId, Instant validatedAt);

    /** Celles de ces notions que l'utilisateur a validées. */
    Set<UUID> validated(UUID userId, Collection<UUID> notionIds);
}
