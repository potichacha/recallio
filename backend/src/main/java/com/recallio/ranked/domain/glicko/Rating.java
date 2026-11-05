package com.recallio.ranked.domain.glicko;

/**
 * Une cote Glicko-2 (ADR 0046), à l'échelle habituelle : 1500 au départ.
 *
 * @param deviation l'incertitude (RD) : haute pour un joueur neuf ou revenu d'une longue pause, elle fond avec les parties
 * @param volatility à quel point les résultats du joueur varient d'une période à l'autre
 */
public record Rating(double rating, double deviation, double volatility) {

    public static final double START_RATING = 1500;
    public static final double START_DEVIATION = 350;
    public static final double START_VOLATILITY = 0.06;

    /** La cote d'un joueur, ou d'une carte, qui n'a encore jamais joué. */
    public static final Rating START = new Rating(START_RATING, START_DEVIATION, START_VOLATILITY);

    public Rating {
        if (deviation <= 0 || volatility <= 0) {
            throw new IllegalArgumentException(
                    "Une RD et une volatilité sont positives : " + deviation + ", " + volatility);
        }
    }
}
