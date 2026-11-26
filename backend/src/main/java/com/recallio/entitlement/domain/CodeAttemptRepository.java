package com.recallio.entitlement.domain;

import java.time.Instant;
import java.util.UUID;

/** Les essais ratés de chaque compte (ADR 0079) : une ligne par compte, jamais le code essayé. */
public interface CodeAttemptRepository {

    /** Vrai si le compte a épuisé ses essais ({@link CodeAttempts#isBlockedAt}). */
    boolean isBlocked(UUID userId, Instant now);

    /**
     * Compte un essai raté de plus ({@link CodeAttempts#afterFailureAt}). La ligne du compte est verrouillée le temps de
     * le compter : deux essais simultanés comptent tous les deux.
     */
    void recordFailure(UUID userId, Instant now);
}
