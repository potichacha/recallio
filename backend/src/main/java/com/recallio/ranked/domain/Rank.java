package com.recallio.ranked.domain;

/**
 * Le rang d'un joueur dans un sujet (ADR 0046), d'après sa cote cachée : Bronze sous 1300, puis une tranche de 200
 * points par rang, Maître à partir de 2100. Chaque rang sauf Maître a quatre divisions de 50 points, de IV (la plus
 * basse) à I.
 *
 * @param division de 4 à 1 ; 0 pour Maître, qui n'en a pas
 */
public record Rank(Tier tier, int division) {

    static final double BRONZE_CEILING = 1300;
    static final double TIER_WIDTH = 200;
    static final double DIVISION_WIDTH = 50;
    static final double MASTER_FLOOR = 2100;
    static final int DIVISIONS = 4;

    public enum Tier {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        DIAMOND,
        MASTER
    }

    public static Rank of(double rating) {
        if (rating >= MASTER_FLOOR) {
            return new Rank(Tier.MASTER, 0);
        }
        int tier = rating < BRONZE_CEILING ? 0 : 1 + (int) ((rating - BRONZE_CEILING) / TIER_WIDTH);
        // Bronze couvre toutes les cotes sous 1300 ; ses divisions vont de 1100 à 1300, IV en dessous.
        double floor = BRONZE_CEILING + (tier - 1) * TIER_WIDTH;
        int step = Math.clamp((long) Math.floor((rating - floor) / DIVISION_WIDTH), 0, DIVISIONS - 1);
        return new Rank(Tier.values()[tier], DIVISIONS - step);
    }

    /** Vrai si ce rang est au-dessus de l'autre : un rang plus haut, ou une meilleure division du même rang. */
    public boolean isAbove(Rank other) {
        if (tier != other.tier) {
            return tier.compareTo(other.tier) > 0;
        }
        return division < other.division;
    }
}
