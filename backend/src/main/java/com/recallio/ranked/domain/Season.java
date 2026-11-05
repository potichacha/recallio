package com.recallio.ranked.domain;

import com.recallio.ranked.domain.glicko.Rating;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Une saison du classé (ADR 0046) : un trimestre civil, en UTC. Au premier duel d'une saison, la cote se rapproche de
 * 1500 de moitié, l'incertitude remonte, et le placement recommence.
 *
 * @param quarter de 1 à 4
 */
public record Season(int year, int quarter) {

    static final int MONTHS_PER_QUARTER = 3;

    /** L'incertitude d'une cote en début de saison, au moins : de quoi bouger vite pendant le placement. */
    static final double RESET_DEVIATION = 200;

    public Season {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Un trimestre va de 1 à 4 : " + quarter);
        }
    }

    public static Season of(Instant instant) {
        LocalDate day = LocalDate.ofInstant(instant, ZoneOffset.UTC);
        return new Season(day.getYear(), (day.getMonthValue() - 1) / MONTHS_PER_QUARTER + 1);
    }

    /** « 2026-T4 » → la saison ; c'est aussi la forme stockée. */
    public static Season parse(String id) {
        String[] parts = id.split("-T");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Saison mal formée : " + id);
        }
        return new Season(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public String id() {
        return year + "-T" + quarter;
    }

    /** Le premier jour de la saison suivante, où celle-ci se termine. */
    public LocalDate endsOn() {
        return LocalDate.of(year, (quarter - 1) * MONTHS_PER_QUARTER + 1, 1).plusMonths(MONTHS_PER_QUARTER);
    }

    /** La cote d'un joueur au premier duel de cette saison, d'après celle de la saison d'avant. */
    public static Rating softReset(Rating previous) {
        double rating = Rating.START_RATING + (previous.rating() - Rating.START_RATING) / 2;
        return new Rating(rating, Math.max(previous.deviation(), RESET_DEVIATION), previous.volatility());
    }
}
