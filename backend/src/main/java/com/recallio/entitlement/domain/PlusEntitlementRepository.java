package com.recallio.entitlement.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Le droit « Plus » de chaque compte (ADR 0079). */
public interface PlusEntitlementRepository {

    /** Le droit du compte, en cours ou échu ; vide s'il n'en a jamais eu. */
    Optional<PlusEntitlement> find(UUID userId);

    /**
     * Le droit du compte, verrouillé jusqu'à la fin de la transaction : deux codes utilisés en même temps se cumulent,
     * sans que l'un efface l'autre. Un compte qui n'en a jamais eu reçoit un droit qui finit à {@code now}, donc échu.
     */
    PlusEntitlement lock(UUID userId, Instant now);

    void save(UUID userId, PlusEntitlement entitlement, Instant now);
}
