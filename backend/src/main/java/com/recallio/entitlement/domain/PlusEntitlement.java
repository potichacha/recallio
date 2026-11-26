package com.recallio.entitlement.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Le droit « Plus » d'un compte (ADR 0079) : une date de fin. Passé cette date, le droit est échu, et le compte
 * retrouve l'offre gratuite sans rien perdre.
 */
public record PlusEntitlement(Instant endsAt) {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    public boolean isActiveAt(Instant now) {
        return endsAt.isAfter(now);
    }

    /**
     * Le droit prolongé de {@code days} jours : ils s'ajoutent à ce qu'il reste, ou partent de maintenant si le droit est
     * échu. Un code utilisé pendant un droit en cours ne fait rien perdre.
     */
    public PlusEntitlement extendedBy(int days, Instant now) {
        if (days <= 0) {
            throw new IllegalArgumentException("Un droit se prolonge d'au moins un jour");
        }
        Instant from = isActiveAt(now) ? endsAt : now;
        return new PlusEntitlement(from.plus(days, ChronoUnit.DAYS));
    }

    /** Les jours qu'il reste, un jour entamé comptant pour un ; 0 si le droit est échu. */
    public int daysLeftAt(Instant now) {
        if (!isActiveAt(now)) {
            return 0;
        }
        long seconds = Duration.between(now, endsAt).toSeconds();
        return Math.toIntExact(Math.max(1, (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY));
    }
}
