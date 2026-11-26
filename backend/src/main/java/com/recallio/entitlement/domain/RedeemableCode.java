package com.recallio.entitlement.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Un code retrouvé par son empreinte, avec son lot et le nombre de comptes qui l'ont déjà utilisé.
 *
 * @param redemptions les utilisations déjà comptées, y compris par des comptes supprimés depuis
 */
public record RedeemableCode(UUID id, AccessCodeBatch batch, int redemptions) {

    /** Vrai si un compte de plus peut l'utiliser : son lot est ouvert, et il lui reste une utilisation. */
    public boolean isRedeemableAt(Instant now) {
        return batch.isOpenAt(now) && redemptions < batch.maxRedemptionsPerCode();
    }
}
