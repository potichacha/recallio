package com.recallio.review.domain.fsrs;

import java.time.Duration;
import java.util.List;

/**
 * Réglages du planificateur FSRS-6 (ADR 0011).
 *
 * @param weights les 21 poids du modèle ; les valeurs par défaut sont celles de FSRS-6, entraînées sur des millions de
 *     révisions Anki
 * @param desiredRetention probabilité de se souvenir visée au moment de la révision (0,9 = 90 %)
 * @param learningSteps intervalles des premières révisions d'une carte nouvelle
 * @param relearningSteps intervalles après un oubli
 * @param maximumIntervalDays intervalle maximal entre deux révisions
 */
public record FsrsParameters(
        List<Double> weights,
        double desiredRetention,
        List<Duration> learningSteps,
        List<Duration> relearningSteps,
        int maximumIntervalDays) {

    public static final int WEIGHT_COUNT = 21;

    private static final List<Double> DEFAULT_WEIGHTS = List.of(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629,
            1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542);
    private static final double DEFAULT_RETENTION = 0.9;
    private static final int DEFAULT_MAXIMUM_INTERVAL_DAYS = 36_500;

    public static final FsrsParameters DEFAULT = new FsrsParameters(
            DEFAULT_WEIGHTS,
            DEFAULT_RETENTION,
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(10)),
            List.of(Duration.ofMinutes(10)),
            DEFAULT_MAXIMUM_INTERVAL_DAYS);

    public FsrsParameters {
        weights = List.copyOf(weights);
        learningSteps = List.copyOf(learningSteps);
        relearningSteps = List.copyOf(relearningSteps);
        if (weights.size() != WEIGHT_COUNT) {
            throw new IllegalArgumentException("FSRS-6 attend " + WEIGHT_COUNT + " poids, reçu " + weights.size());
        }
        if (desiredRetention <= 0 || desiredRetention >= 1) {
            throw new IllegalArgumentException("La rétention visée doit être comprise entre 0 et 1 (exclus)");
        }
        if (maximumIntervalDays < 1) {
            throw new IllegalArgumentException("L'intervalle maximal doit être d'au moins un jour");
        }
    }

    public FsrsParameters withDesiredRetention(double retention) {
        return new FsrsParameters(weights, retention, learningSteps, relearningSteps, maximumIntervalDays);
    }

    public FsrsParameters withSteps(List<Duration> learning, List<Duration> relearning) {
        return new FsrsParameters(weights, desiredRetention, learning, relearning, maximumIntervalDays);
    }
}
