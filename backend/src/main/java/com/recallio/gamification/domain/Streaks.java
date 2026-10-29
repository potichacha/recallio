package com.recallio.gamification.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

/**
 * Calcul du streak (CLAUDE.md, section 6.3) : le nombre de jours consécutifs où l'objectif quotidien a été atteint,
 * jusqu'à aujourd'hui. Les jours sont ceux du fuseau de l'utilisateur.
 *
 * <p>Tant que la journée n'est pas finie, le streak d'hier reste valable : on peut encore atteindre l'objectif. Un
 * jour entier manqué le remet à zéro.
 */
public final class Streaks {

    private Streaks() {}

    /**
     * @param goalDays jours (dans le fuseau de l'utilisateur) où l'objectif a été atteint
     * @param today aujourd'hui, dans ce même fuseau
     */
    public static int current(Collection<LocalDate> goalDays, LocalDate today) {
        Set<LocalDate> days = Set.copyOf(goalDays);
        LocalDate day = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
