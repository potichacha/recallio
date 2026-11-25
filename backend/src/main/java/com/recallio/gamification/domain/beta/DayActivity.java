package com.recallio.gamification.domain.beta;

import com.recallio.gamification.domain.beta.BetaReport.Window;
import com.recallio.platform.domain.UsageStatistics.DailyCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * L'activité d'un jour (ADR 0076). Les trois chiffres de la révision suivent le jour de chacun dans son fuseau, comme
 * le streak ; les comptes créés et les notions validées suivent le jour UTC.
 *
 * @param reviewers les comptes qui ont révisé au moins une carte
 * @param reviews les cartes révisées
 * @param goalsReached les comptes qui ont atteint l'objectif du jour
 * @param tourValidations les notions validées dans le Tour
 */
public record DayActivity(
        LocalDate day, long reviewers, long reviews, long goalsReached, long accountsCreated, long tourValidations) {

    /** Un jour par jour de la fenêtre, du plus ancien au plus récent ; un jour sans rien compte 0 partout. */
    public static List<DayActivity> series(Window window, Map<DailyCount, Map<LocalDate, Long>> counts) {
        return window.days().stream()
                .map(day -> new DayActivity(
                        day,
                        count(counts, DailyCount.REVIEWERS, day),
                        count(counts, DailyCount.REVIEWS, day),
                        count(counts, DailyCount.GOALS_REACHED, day),
                        count(counts, DailyCount.ACCOUNTS_CREATED, day),
                        count(counts, DailyCount.TOUR_VALIDATIONS, day)))
                .toList();
    }

    private static long count(Map<DailyCount, Map<LocalDate, Long>> counts, DailyCount which, LocalDate day) {
        return counts.getOrDefault(which, Map.of()).getOrDefault(day, 0L);
    }
}
