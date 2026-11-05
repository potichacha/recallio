package com.recallio.ranked.domain.glicko;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Glicko-2 (Mark Glickman, « Example of the Glicko-2 system », 2012), en Java pur : la cote après une période de
 * notation. Ici, une période vaut un duel pour un joueur, une réponse pour une carte (ADR 0046). Les étapes suivent
 * celles de l'article, et les noms ses lettres grecques.
 */
public final class Glicko2 {

    /** Entre l'échelle habituelle (1500, 350) et celle des calculs. */
    static final double SCALE = 173.7178;

    /** La contrainte sur la volatilité : 0,5 est la valeur que l'article conseille pour la plupart des jeux. */
    static final double TAU = 0.5;

    private static final double CONVERGENCE = 0.000001;

    private Glicko2() {}

    /** La cote du joueur après ces parties ; sans partie, seule son incertitude grandit. */
    public static Rating update(Rating player, List<Game> games) {
        double mu = (player.rating() - Rating.START_RATING) / SCALE;
        double phi = player.deviation() / SCALE;
        double sigma = player.volatility();
        if (games.isEmpty()) {
            return new Rating(player.rating(), deviation(Math.sqrt(phi * phi + sigma * sigma)), sigma);
        }
        // Étapes 3 et 4 : la variance estimée v, et l'amélioration estimée Δ (ici sa somme, avant le facteur v).
        double inverseVariance = 0;
        double improvement = 0;
        for (Game game : games) {
            double muJ = (game.opponent().rating() - Rating.START_RATING) / SCALE;
            double g = g(game.opponent().deviation() / SCALE);
            double expected = expected(mu, muJ, g);
            inverseVariance += g * g * expected * (1 - expected);
            improvement += g * (game.score() - expected);
        }
        double v = 1 / inverseVariance;
        double delta = v * improvement;
        // Étapes 5 à 7 : la nouvelle volatilité, puis la nouvelle incertitude et la nouvelle cote.
        double newSigma = volatility(phi, sigma, v, delta);
        double phiStar = Math.sqrt(phi * phi + newSigma * newSigma);
        double newPhi = 1 / Math.sqrt(1 / (phiStar * phiStar) + 1 / v);
        double newMu = mu + newPhi * newPhi * improvement;
        return new Rating(SCALE * newMu + Rating.START_RATING, deviation(newPhi), newSigma);
    }

    /** L'espérance de gain du joueur contre cet adversaire, de 0 à 1. */
    public static double expectedScore(Rating player, Rating opponent) {
        double mu = (player.rating() - Rating.START_RATING) / SCALE;
        double muJ = (opponent.rating() - Rating.START_RATING) / SCALE;
        return expected(mu, muJ, g(opponent.deviation() / SCALE));
    }

    /** Une incertitude ne dépasse jamais celle d'un joueur neuf. */
    private static double deviation(double phi) {
        return Math.min(SCALE * phi, Rating.START_DEVIATION);
    }

    private static double g(double phi) {
        return 1 / Math.sqrt(1 + 3 * phi * phi / (Math.PI * Math.PI));
    }

    private static double expected(double mu, double muJ, double g) {
        return 1 / (1 + Math.exp(-g * (mu - muJ)));
    }

    /** Étape 5 : la nouvelle volatilité, par la méthode d'Illinois, comme dans l'article. */
    private static double volatility(double phi, double sigma, double v, double delta) {
        double logVariance = Math.log(sigma * sigma);
        DoubleUnaryOperator f = x -> {
            double ex = Math.exp(x);
            double denominator = phi * phi + v + ex;
            return ex * (delta * delta - phi * phi - v - ex) / (2 * denominator * denominator)
                    - (x - logVariance) / (TAU * TAU);
        };
        double left = logVariance;
        double right;
        if (delta * delta > phi * phi + v) {
            right = Math.log(delta * delta - phi * phi - v);
        } else {
            int k = 1;
            while (f.applyAsDouble(logVariance - k * TAU) < 0) {
                k++;
            }
            right = logVariance - k * TAU;
        }
        double fLeft = f.applyAsDouble(left);
        double fRight = f.applyAsDouble(right);
        while (Math.abs(right - left) > CONVERGENCE) {
            double middle = left + (left - right) * fLeft / (fRight - fLeft);
            double fMiddle = f.applyAsDouble(middle);
            if (fMiddle * fRight <= 0) {
                left = right;
                fLeft = fRight;
            } else {
                fLeft = fLeft / 2;
            }
            right = middle;
            fRight = fMiddle;
        }
        return Math.exp(left / 2);
    }

    /**
     * Une partie de la période.
     *
     * @param score 1 pour une victoire, 0,5 pour un nul, 0 pour une défaite
     */
    public record Game(Rating opponent, double score) {

        public static final double WIN = 1;
        public static final double DRAW = 0.5;
        public static final double LOSS = 0;
    }
}
