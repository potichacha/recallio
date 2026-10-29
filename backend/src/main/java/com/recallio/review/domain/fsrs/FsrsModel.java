package com.recallio.review.domain.fsrs;

import com.recallio.review.domain.Rating;
import java.util.List;

/**
 * Les formules de FSRS-6, transcrites de l'implémentation de référence py-fsrs (ADR 0011). Les indices {@code w(n)}
 * suivent la numérotation des poids de l'algorithme, ce qui permet de comparer ligne à ligne avec la référence.
 *
 * <ul>
 *   <li><b>Stabilité</b> S : nombre de jours au bout duquel la probabilité de se souvenir tombe à 90 %.
 *   <li><b>Difficulté</b> D : entre 1 et 10, elle freine la croissance de la stabilité.
 *   <li><b>Récupérabilité</b> R : probabilité de se souvenir après t jours, {@code R = (1 + F·t/S)^C}.
 * </ul>
 */
final class FsrsModel {

    static final double MIN_STABILITY = 0.001;
    static final double MIN_DIFFICULTY = 1.0;
    static final double MAX_DIFFICULTY = 10.0;

    /** Note de référence des formules : « Bien » ne change pas la difficulté. */
    private static final int NEUTRAL_RATING = 3;
    /** Base de la stabilité de rappel : {@code 11 - D} vaut 1 pour la difficulté maximale. */
    private static final double RECALL_DIFFICULTY_BASE = 11.0;
    /** La stabilité est définie comme le délai au bout duquel R vaut 90 %. */
    private static final double STABILITY_RETENTION = 0.9;

    private final List<Double> weights;
    private final double decay;
    private final double factor;
    private final double desiredRetention;
    private final int maximumIntervalDays;

    FsrsModel(FsrsParameters parameters) {
        this.weights = parameters.weights();
        this.decay = -w(20);
        this.factor = Math.pow(STABILITY_RETENTION, 1 / decay) - 1;
        this.desiredRetention = parameters.desiredRetention();
        this.maximumIntervalDays = parameters.maximumIntervalDays();
    }

    double initialStability(Rating rating) {
        return clampStability(w(rating.value() - 1));
    }

    double initialDifficulty(Rating rating) {
        return clampDifficulty(rawInitialDifficulty(rating));
    }

    /** La difficulté évolue selon la note, amortie près de 10, puis revient légèrement vers celle d'une carte facile. */
    double nextDifficulty(double difficulty, Rating rating) {
        double delta = -w(6) * (rating.value() - NEUTRAL_RATING);
        double damped = difficulty + (MAX_DIFFICULTY - difficulty) * delta / (MAX_DIFFICULTY - 1);
        double meanReverted = w(7) * rawInitialDifficulty(Rating.EASY) + (1 - w(7)) * damped;
        return clampDifficulty(meanReverted);
    }

    /** Probabilité de se souvenir après {@code elapsedDays} jours. */
    double retrievability(long elapsedDays, double stability) {
        return Math.pow(1 + factor * Math.max(0, elapsedDays) / stability, decay);
    }

    /** Révision espacée d'au moins un jour : la stabilité croît si la carte est retrouvée, chute si elle est oubliée. */
    double nextStability(double difficulty, double stability, double retrievability, Rating rating) {
        double next = rating == Rating.AGAIN
                ? forgetStability(difficulty, stability, retrievability)
                : recallStability(difficulty, stability, retrievability, rating);
        return clampStability(next);
    }

    /** Révision le jour même (étapes d'apprentissage) : la stabilité évolue peu. */
    double shortTermStability(double stability, Rating rating) {
        double increase = Math.exp(w(17) * (rating.value() - NEUTRAL_RATING + w(18))) * Math.pow(stability, -w(19));
        if (rating != Rating.AGAIN) {
            increase = Math.max(increase, 1.0);
        }
        return clampStability(stability * increase);
    }

    /** Intervalle, en jours entiers, au bout duquel la récupérabilité atteint la rétention visée. */
    int intervalDays(double stability) {
        double days = stability / factor * (Math.pow(desiredRetention, 1 / decay) - 1);
        // Math.rint arrondit au pair le plus proche, comme round() en Python : mêmes résultats que la référence.
        long rounded = (long) Math.rint(days);
        return (int) Math.clamp(rounded, 1, maximumIntervalDays);
    }

    private double recallStability(double difficulty, double stability, double retrievability, Rating rating) {
        double hardPenalty = rating == Rating.HARD ? w(15) : 1;
        double easyBonus = rating == Rating.EASY ? w(16) : 1;
        return stability
                * (1
                        + Math.exp(w(8))
                                * (RECALL_DIFFICULTY_BASE - difficulty)
                                * Math.pow(stability, -w(9))
                                * (Math.exp((1 - retrievability) * w(10)) - 1)
                                * hardPenalty
                                * easyBonus);
    }

    private double forgetStability(double difficulty, double stability, double retrievability) {
        double longTerm = w(11)
                * Math.pow(difficulty, -w(12))
                * (Math.pow(stability + 1, w(13)) - 1)
                * Math.exp((1 - retrievability) * w(14));
        double shortTerm = stability / Math.exp(w(17) * w(18));
        return Math.min(longTerm, shortTerm);
    }

    private double rawInitialDifficulty(Rating rating) {
        return w(4) - Math.exp(w(5) * (rating.value() - 1)) + 1;
    }

    private double w(int index) {
        return weights.get(index);
    }

    private static double clampStability(double stability) {
        return Math.max(stability, MIN_STABILITY);
    }

    private static double clampDifficulty(double difficulty) {
        return Math.clamp(difficulty, MIN_DIFFICULTY, MAX_DIFFICULTY);
    }
}
