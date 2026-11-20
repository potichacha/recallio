package com.recallio.review.domain.daily;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Les chiffres du jour d'un défi (ADR 0067), agrégés, sans aucun nom : combien l'ont fini, leur score moyen et la
 * répartition des scores. Un joueur ne les voit qu'une fois son propre défi fini, pour qu'ils ne l'influencent pas.
 *
 * @param players les joueurs qui ont fini le défi
 * @param averageScore leur score moyen, à une décimale ; 0 sans joueur
 * @param playersByScore le nombre de joueurs de chaque score, de 0 au nombre de questions
 */
public record DailyChallengeStats(int players, double averageScore, List<Integer> playersByScore) {

    /** Le score moyen se donne à une décimale : « 3,4 sur 5 ». */
    private static final int AVERAGE_DECIMALS = 1;

    public DailyChallengeStats {
        playersByScore = List.copyOf(playersByScore);
    }

    /**
     * @param playersByScore le nombre de joueurs de chaque score obtenu ; un score absent n'a aucun joueur
     * @param questions le nombre de questions du défi, le meilleur score possible
     */
    public static DailyChallengeStats of(Map<Integer, Integer> playersByScore, int questions) {
        List<Integer> distribution = IntStream.rangeClosed(0, questions)
                .mapToObj(score -> playersByScore.getOrDefault(score, 0))
                .toList();
        int players = distribution.stream().mapToInt(Integer::intValue).sum();
        long points = IntStream.rangeClosed(0, questions)
                .mapToLong(score -> (long) score * distribution.get(score))
                .sum();
        double average = players == 0
                ? 0
                : BigDecimal.valueOf(points)
                        .divide(BigDecimal.valueOf(players), AVERAGE_DECIMALS, RoundingMode.HALF_UP)
                        .doubleValue();
        return new DailyChallengeStats(players, average, distribution);
    }
}
