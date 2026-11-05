package com.recallio.gamification.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La rétention (ADR 0050) : parmi les joueurs dont le premier jour actif est assez ancien, la part revenue réviser N jours
 * plus tard, jour pour jour (J1, J7, J30). C'est la mesure que se donnait la phase 1 : combien reviennent après 7 jours.
 *
 * @param players les joueurs qui ont révisé au moins une fois
 * @param days la rétention à chacun des jours mesurés
 */
public record RetentionReport(int players, List<DayRetention> days) {

    /** Les jours mesurés après le premier jour actif. */
    public static final List<Integer> DAYS = List.of(1, 7, 30);

    public RetentionReport {
        days = List.copyOf(days);
    }

    /** La rétention, d'après les jours actifs de chaque joueur, aujourd'hui compris. */
    public static RetentionReport of(Map<UUID, Set<LocalDate>> activeDays, LocalDate today) {
        Collection<Set<LocalDate>> players =
                activeDays.values().stream().filter(days -> !days.isEmpty()).toList();
        return new RetentionReport(
                players.size(),
                DAYS.stream().map(day -> retention(players, day, today)).toList());
    }

    private static DayRetention retention(Collection<Set<LocalDate>> players, int day, LocalDate today) {
        int eligible = 0;
        int retained = 0;
        for (Set<LocalDate> active : players) {
            LocalDate target = Collections.min(active).plusDays(day);
            if (!target.isAfter(today)) {
                eligible++;
                if (active.contains(target)) {
                    retained++;
                }
            }
        }
        return new DayRetention(day, eligible, retained);
    }

    /**
     * @param eligible les joueurs dont le premier jour actif remonte à au moins {@code day} jours
     * @param retained ceux d'entre eux qui ont révisé ce jour-là
     */
    public record DayRetention(int day, int eligible, int retained) {

        /** La part des joueurs revenus, en pourcentage arrondi vers le bas ; 0 tant que personne n'est éligible. */
        public int percent() {
            return eligible == 0 ? 0 : retained * 100 / eligible;
        }
    }
}
