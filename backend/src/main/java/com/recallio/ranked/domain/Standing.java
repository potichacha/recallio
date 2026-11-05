package com.recallio.ranked.domain;

import com.recallio.ranked.domain.Rank.Tier;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import com.recallio.ranked.domain.glicko.Rating;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * La place d'un joueur dans le classé d'un sujet, pour une saison (ADR 0046).
 *
 * @param seasonDuels les duels joués cette saison : les {@value #PLACEMENT_DUELS} premiers sont le placement
 * @param peakRating la meilleure cote de la saison, placement fini ; {@code null} pendant le placement
 */
public record Standing(
        UUID userId,
        String subjectSlug,
        Season season,
        Rating rating,
        int seasonDuels,
        int wins,
        int losses,
        int draws,
        @Nullable Double peakRating) {

    public static final int PLACEMENT_DUELS = 5;

    public static Standing start(UUID userId, String subjectSlug, Season season) {
        return new Standing(userId, subjectSlug, season, Rating.START, 0, 0, 0, 0, null);
    }

    public boolean isPlacing() {
        return seasonDuels < PLACEMENT_DUELS;
    }

    /** Le rang affiché ; aucun pendant le placement. */
    public Optional<Rank> rank() {
        return isPlacing() ? Optional.empty() : Optional.of(Rank.of(rating.rating()));
    }

    /** Le meilleur rang de la saison, sa récompense ; aucun tant que le placement n'est pas fini. */
    public Optional<Tier> peakTier() {
        Double peak = peakRating;
        return peak == null ? Optional.empty() : Optional.of(Rank.of(peak).tier());
    }

    /** La place au début de la saison donnée : celle d'avant se rapproche de 1500, et le placement recommence. */
    public Standing inSeason(Season current) {
        if (season.equals(current)) {
            return this;
        }
        return new Standing(userId, subjectSlug, current, Season.softReset(rating), 0, 0, 0, 0, null);
    }

    /**
     * La place après un duel de la saison.
     *
     * @param score celui du joueur : {@link Game#WIN}, {@link Game#DRAW} ou {@link Game#LOSS}
     */
    public Standing after(Rating newRating, double score) {
        int duels = seasonDuels + 1;
        Double peak = peakRating;
        Double newPeak = duels < PLACEMENT_DUELS
                ? null
                : Double.valueOf(peak == null ? newRating.rating() : Math.max(peak, newRating.rating()));
        return new Standing(
                userId,
                subjectSlug,
                season,
                newRating,
                duels,
                wins + (score == Game.WIN ? 1 : 0),
                losses + (score == Game.LOSS ? 1 : 0),
                draws + (score == Game.DRAW ? 1 : 0),
                newPeak);
    }
}
