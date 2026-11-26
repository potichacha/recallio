package com.recallio.review.domain.drill;

import org.jspecify.annotations.Nullable;

/**
 * Ce qu'un compte a fait corriger aujourd'hui en révision intensive, et ce qu'il peut encore (ADR 0079).
 *
 * @param dailyLimit le nombre de réponses par jour d'un compte sans Plus ; {@code null} avec Plus, qui n'a pas de limite
 */
public record DrillQuota(DrillUsage usage, @Nullable Integer dailyLimit) {

    /** Les réponses qu'il reste aujourd'hui, jamais négatives ; {@code null} sans limite. */
    public @Nullable Integer remaining() {
        Integer limit = dailyLimit;
        return limit == null ? null : Math.max(0, limit - usage.answers());
    }
}
