package com.recallio.gamification.domain.funnel;

import com.recallio.gamification.domain.leaderboard.Week;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * Le parcours mesuré (ADR 0064) : semaine par semaine, combien de visites, d'essais commencés, de comptes créés et de
 * premières révisions, et le taux de passage de chaque étape à la suivante.
 *
 * <p>Les événements ne désignent personne : un taux compare deux comptes de la même semaine, pas le devenir des mêmes
 * visiteurs. Il peut donc dépasser 100 % (un compte créé le lundi par un visiteur venu le dimanche).
 *
 * @param weeks les semaines mesurées, la plus récente d'abord
 */
public record FunnelReport(List<FunnelWeek> weeks) {

    /** Le nombre de semaines mesurées : deux mois, de quoi voir une tendance. */
    public static final int WEEKS = 8;

    /** Le fuseau des semaines : un événement n'a pas d'utilisateur, donc pas de fuseau à lui. */
    public static final ZoneId ZONE = ZoneOffset.UTC;

    private static final int PERCENT = 100;

    public FunnelReport {
        weeks = List.copyOf(weeks);
    }

    /**
     * Les {@link #WEEKS} semaines ISO (du lundi au dimanche, en UTC) à mesurer à cet instant, la semaine en cours
     * d'abord.
     */
    public static List<Week> weeksUpTo(Instant now) {
        Week current = Week.containing(LocalDate.ofInstant(now, ZONE));
        return IntStream.range(0, WEEKS)
                .mapToObj(weeksAgo -> new Week(current.monday().minusWeeks(weeksAgo)))
                .toList();
    }

    /** @param steps chaque étape du parcours, dans l'ordre */
    public record FunnelWeek(Week week, List<StepCount> steps) {

        public FunnelWeek {
            steps = List.copyOf(steps);
        }

        /** Les comptes d'une semaine ; une étape absente de {@code counts} compte 0. */
        public static FunnelWeek of(Week week, Map<FunnelStep, Long> counts) {
            List<StepCount> steps = new ArrayList<>();
            long previous = 0;
            for (FunnelStep step : FunnelStep.values()) {
                long count = counts.getOrDefault(step, 0L);
                steps.add(new StepCount(step, count, steps.isEmpty() ? null : percent(count, previous)));
                previous = count;
            }
            return new FunnelWeek(week, steps);
        }

        /** {@code count} rapporté à {@code previous}, en pourcentage arrondi vers le bas ; aucun si personne avant. */
        private static @Nullable Integer percent(long count, long previous) {
            return previous == 0 ? null : Math.toIntExact(count * PERCENT / previous);
        }
    }

    /**
     * @param count les événements de l'étape cette semaine-là
     * @param percent le taux de passage depuis l'étape précédente, en pourcentage arrondi vers le bas ; {@code null}
     *     pour la première étape, ou quand personne n'a franchi la précédente cette semaine-là
     */
    public record StepCount(
            FunnelStep step, long count, @Nullable Integer percent) {}
}
