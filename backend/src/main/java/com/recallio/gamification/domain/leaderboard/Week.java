package com.recallio.gamification.domain.leaderboard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Une semaine du lundi au dimanche (ISO 8601), celle d'un classement hebdomadaire. */
public record Week(LocalDate monday) {

    public Week {
        if (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Une semaine commence un lundi : " + monday);
        }
    }

    public static Week containing(LocalDate day) {
        return new Week(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    public LocalDate sunday() {
        return monday.plusDays(DayOfWeek.values().length - 1);
    }

    /** Le premier instant de la semaine dans ce fuseau. */
    public Instant startIn(ZoneId zone) {
        return monday.atStartOfDay(zone).toInstant();
    }

    /** Le premier instant de la semaine suivante dans ce fuseau (exclu). */
    public Instant endIn(ZoneId zone) {
        return monday.plusWeeks(1).atStartOfDay(zone).toInstant();
    }
}
