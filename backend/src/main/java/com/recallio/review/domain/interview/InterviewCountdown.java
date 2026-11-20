package com.recallio.review.domain.interview;

import com.recallio.content.NotionInfo;
import com.recallio.review.domain.track.MasteryLevel;
import com.recallio.review.domain.track.NewCardOrder;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.Readiness;
import com.recallio.user.LearningTrack;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Le compte à rebours avant un entretien (ADR 0066) : les jours qui restent, les notions essentielles à acquérir cette
 * semaine, et si l'on est en avance ou en retard. L'indice de préparation (ADR 0041) dit si l'on est prêt ; le compte à
 * rebours dit comment y arriver à temps.
 *
 * <p>Tout se recalcule à chaque lecture d'après la maîtrise des notions : seul le plan ({@link InterviewPlan}) est
 * gardé.
 *
 * <ul>
 *   <li>Les semaines comptent depuis le jour où la date a été fixée, pas celles du calendrier ; la dernière s'arrête la
 *       veille de l'entretien.
 *   <li>Les notions de la ligne de départ sont réparties sur toutes les semaines, sauf la dernière quand il y en a au
 *       moins deux : elle sert à réviser, puisque la maîtrise vient des révisions espacées.
 *   <li>L'objectif cumulé de la semaine {@code k} est {@code ceil(départ × k / semaines de notions)} notions acquises.
 *       Ce qui est fait se mesure sur ce qui reste ({@code départ - restantes}) : une notion qui repasse sous
 *       « acquise », ou qui s'ajoute au contenu, fait reculer.
 * </ul>
 *
 * @param daysLeft les jours jusqu'à l'entretien : 0 le jour même, et après
 * @param readiness l'indice de préparation du sujet
 * @param week la semaine en cours, de 1 à {@code weeks}
 * @param weeks les semaines du plan, jusqu'à l'entretien
 * @param thisWeek la semaine en cours
 * @param nextWeeks les semaines suivantes, dans l'ordre
 * @param essentialsLeft les notions essentielles pas encore acquises
 */
public record InterviewCountdown(
        InterviewPlan plan,
        int daysLeft,
        InterviewStatus status,
        Readiness readiness,
        int week,
        int weeks,
        ThisWeek thisWeek,
        List<PlannedWeek> nextWeeks,
        int essentialsLeft) {

    public static final int DAYS_PER_WEEK = 7;

    /** À partir de ce nombre de semaines, la dernière sert à réviser, sans nouvelle notion. */
    public static final int WEEKS_WITH_A_REVIEW_WEEK = 2;

    private static final int FIRST_WEEK = 1;

    public InterviewCountdown {
        nextWeeks = List.copyOf(nextWeeks);
    }

    /**
     * Les notions du plan : celles, essentielles, qui ne sont pas encore acquises, dans l'ordre où le parcours « préparer
     * un entretien » sert leurs cartes (ADR 0032) : les notions connues au test de niveau (ADR 0068) après les autres de
     * même priorité.
     *
     * @param notions la maîtrise des notions d'un sujet
     * @param knownNotions les notions que l'utilisateur a montré connaître au test de niveau
     */
    public static List<PlanNotion> toAcquire(Map<NotionInfo, NotionMastery> notions, Set<UUID> knownNotions) {
        return notions.entrySet().stream()
                .filter(entry -> entry.getKey().priority() == Readiness.ESSENTIAL_PRIORITY)
                .filter(entry -> entry.getValue().level().compareTo(MasteryLevel.ACQUIRED) < 0)
                .sorted(Map.Entry.comparingByKey(NewCardOrder.notionOrder(LearningTrack.PREPARE, knownNotions)))
                .map(entry -> new PlanNotion(
                        entry.getKey().slug(),
                        entry.getKey().title(),
                        entry.getValue().level()))
                .toList();
    }

    /**
     * @param today le jour de l'utilisateur, dans son fuseau
     * @param toAcquire les notions du plan qui restent à acquérir aujourd'hui ({@link #toAcquire(Map, Set)})
     * @param readiness l'indice de préparation du sujet
     */
    public static InterviewCountdown of(
            InterviewPlan plan, LocalDate today, List<PlanNotion> toAcquire, Readiness readiness) {
        Schedule schedule = Schedule.of(plan);
        int week = schedule.weekOf(today);
        int left = toAcquire.size();
        int done = Math.max(0, plan.notionsToAcquire() - left);
        int quota = Math.max(0, schedule.target(week) - done);
        var thisWeek = new ThisWeek(
                schedule.startOf(week),
                schedule.endOf(week),
                schedule.isReviewWeek(week),
                quota,
                toAcquire.subList(0, Math.min(quota, left)));
        List<PlannedWeek> nextWeeks = IntStream.rangeClosed(week + 1, schedule.weeks())
                .mapToObj(schedule::planned)
                .toList();
        InterviewStatus status = status(
                today.isAfter(plan.interviewOn()),
                readiness.ready(),
                week > FIRST_WEEK && done < schedule.target(week - 1),
                quota == 0 && left > 0);
        int daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(today, plan.interviewOn()));
        return new InterviewCountdown(
                plan, daysLeft, status, readiness, week, schedule.weeks(), thisWeek, nextWeeks, left);
    }

    /** Le premier statut qui s'applique, dans l'ordre de {@link InterviewStatus}. */
    private static InterviewStatus status(boolean passed, boolean ready, boolean pastWeekUnfinished, boolean ahead) {
        if (passed) {
            return InterviewStatus.PASSED;
        }
        if (ready) {
            return InterviewStatus.READY;
        }
        if (pastWeekUnfinished) {
            return InterviewStatus.BEHIND;
        }
        return ahead ? InterviewStatus.AHEAD : InterviewStatus.ON_TRACK;
    }

    /**
     * Une notion du plan.
     *
     * @param mastery son niveau de maîtrise, en dessous d'« acquise »
     */
    public record PlanNotion(String slug, String title, MasteryLevel mastery) {}

    /**
     * La semaine en cours.
     *
     * @param review vrai pendant la semaine de révision, la dernière
     * @param quota les notions à acquérir avant la fin de la semaine pour tenir le plan, retard compris
     * @param notions les premières notions du plan, autant que le quota
     */
    public record ThisWeek(LocalDate startsOn, LocalDate endsOn, boolean review, int quota, List<PlanNotion> notions) {

        public ThisWeek {
            notions = List.copyOf(notions);
        }
    }

    /**
     * Une semaine à venir.
     *
     * @param number son rang dans le plan, à partir de 1
     * @param review vrai pour la semaine de révision, la dernière
     * @param notions les notions que le plan y prévoit
     */
    public record PlannedWeek(int number, LocalDate startsOn, LocalDate endsOn, boolean review, int notions) {}

    /**
     * Les semaines du plan, de son premier jour à la veille de l'entretien.
     *
     * @param weeks toutes les semaines, la semaine de révision comprise
     * @param notionWeeks celles où l'on acquiert des notions
     * @param notions la ligne de départ
     */
    private record Schedule(LocalDate start, LocalDate interviewOn, int weeks, int notionWeeks, int notions) {

        static Schedule of(InterviewPlan plan) {
            int days = Math.toIntExact(ChronoUnit.DAYS.between(plan.plannedOn(), plan.interviewOn()));
            int weeks = Math.max(FIRST_WEEK, Math.ceilDiv(days, DAYS_PER_WEEK));
            int notionWeeks = weeks >= WEEKS_WITH_A_REVIEW_WEEK ? weeks - 1 : weeks;
            return new Schedule(plan.plannedOn(), plan.interviewOn(), weeks, notionWeeks, plan.notionsToAcquire());
        }

        /** La semaine d'un jour : la dernière le jour de l'entretien et après. */
        int weekOf(LocalDate day) {
            long week = Math.floorDiv(ChronoUnit.DAYS.between(start, day), DAYS_PER_WEEK) + FIRST_WEEK;
            return Math.clamp(week, FIRST_WEEK, weeks);
        }

        /** Les notions acquises depuis le départ qu'attend la fin d'une semaine ; aucune avant la première. */
        int target(int week) {
            return Math.ceilDiv(notions * Math.min(week, notionWeeks), notionWeeks);
        }

        LocalDate startOf(int week) {
            return start.plusDays((long) (week - FIRST_WEEK) * DAYS_PER_WEEK);
        }

        LocalDate endOf(int week) {
            LocalDate end = startOf(week).plusDays(DAYS_PER_WEEK - 1L);
            LocalDate eve = interviewOn.minusDays(1);
            return end.isBefore(eve) ? end : eve;
        }

        boolean isReviewWeek(int week) {
            return weeks >= WEEKS_WITH_A_REVIEW_WEEK && week == weeks;
        }

        PlannedWeek planned(int week) {
            return new PlannedWeek(
                    week, startOf(week), endOf(week), isReviewWeek(week), target(week) - target(week - 1));
        }
    }
}
