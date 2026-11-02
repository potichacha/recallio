package com.recallio.live.domain;

import java.time.Duration;

/**
 * Le rythme d'une partie : le temps de répondre à une question, puis celui de lire la correction avant la suivante.
 */
public record LiveTiming(Duration question, Duration reveal) {

    public LiveTiming {
        if (question.isNegative() || question.isZero() || reveal.isNegative()) {
            throw new IllegalArgumentException("Durées d'une partie invalides : " + question + ", " + reveal);
        }
    }
}
