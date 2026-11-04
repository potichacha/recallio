package com.recallio.gamification.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Le streak et ses gels (CLAUDE.md, section 6.3 ; ADR 0036), recalculés à partir des jours où l'objectif quotidien a
 * été atteint, dans le fuseau de l'utilisateur.
 *
 * <p>Le streak compte les jours consécutifs où l'objectif est atteint. Tant que la journée n'est pas finie, celui d'hier
 * reste valable. Chaque fois qu'il atteint un multiple de {@value #DAYS_PER_FREEZE} jours, il rapporte un gel, jusqu'à
 * {@value #MAX_FREEZES} en réserve. Un jour entier manqué consomme un gel, et le streak continue sans compter ce jour ;
 * sans gel, il retombe à zéro.
 *
 * @param days jours où l'objectif a été atteint depuis le début du streak, sans les jours couverts par un gel
 * @param freezes gels en réserve
 * @param frozenDays jours manqués couverts par un gel, dans l'ordre
 * @param freezeEarnedToday si l'objectif atteint aujourd'hui vient de rapporter un gel
 */
public record Streak(int days, int freezes, List<LocalDate> frozenDays, boolean freezeEarnedToday) {

    /** Jours de streak qui rapportent un gel. */
    public static final int DAYS_PER_FREEZE = 7;

    /** Gels gardés en réserve, au plus. */
    public static final int MAX_FREEZES = 2;

    public Streak {
        frozenDays = List.copyOf(frozenDays);
    }

    /**
     * @param goalDays jours (dans le fuseau de l'utilisateur) où l'objectif a été atteint
     * @param today aujourd'hui, dans ce même fuseau
     */
    public static Streak of(Collection<LocalDate> goalDays, LocalDate today) {
        Set<LocalDate> goals = Set.copyOf(goalDays);
        LocalDate first = goals.stream().min(Comparator.naturalOrder()).orElse(today);
        int days = 0;
        int freezes = 0;
        List<LocalDate> frozenDays = new ArrayList<>();
        boolean freezeEarnedToday = false;
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            if (goals.contains(day)) {
                days++;
                if (days % DAYS_PER_FREEZE == 0 && freezes < MAX_FREEZES) {
                    freezes++;
                    if (day.equals(today)) {
                        freezeEarnedToday = true;
                    }
                }
            } else if (day.isBefore(today) && days > 0) { // aujourd'hui n'est jamais manqué : il n'est pas fini
                if (freezes > 0) {
                    freezes--;
                    frozenDays.add(day);
                } else {
                    days = 0;
                }
            }
        }
        return new Streak(days, freezes, frozenDays, freezeEarnedToday);
    }

    /** Jours où atteindre l'objectif avant le prochain gel ; vide quand la réserve est pleine. */
    public OptionalInt daysToNextFreeze() {
        return freezes < MAX_FREEZES ? OptionalInt.of(DAYS_PER_FREEZE - days % DAYS_PER_FREEZE) : OptionalInt.empty();
    }
}
