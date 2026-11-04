package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.ActivityRepository.DailyActivity;
import com.recallio.gamification.domain.Streaks;
import com.recallio.gamification.domain.SubjectLevel;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.XpLedger.XpTotals;
import com.recallio.gamification.domain.XpRules;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cas d'usage : la progression de l'utilisateur (XP et niveau de ses sujets, streak, objectif du jour). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ProgressService {

    private final ActivityRepository activity;
    private final XpLedger ledger;
    private final FollowedSubjects followedSubjects;
    private final XpRules rules;
    private final Clock clock;

    public ProgressService(
            ActivityRepository activity,
            XpLedger ledger,
            FollowedSubjects followedSubjects,
            GamificationConfig config,
            Clock clock) {
        this.activity = activity;
        this.ledger = ledger;
        this.followedSubjects = followedSubjects;
        this.rules = config.rules();
        this.clock = clock;
    }

    public Progress progress(UserRef user) {
        LocalDate today = LocalDate.now(clock.withZone(user.timezone()));
        List<LocalDate> goalDays = activity.goalDays(user.id());
        XpTotals xp = ledger.totals(user.id());
        // Chaque sujet suivi, même sans XP : un sujet qu'on ne suit plus garde la sienne, et la retrouve si on le
        // suit de nouveau.
        List<SubjectXp> subjects = followedSubjects.of(user).stream()
                .map(slug -> new SubjectXp(slug, SubjectLevel.of(xp.of(slug))))
                .toList();
        return new Progress(
                xp.total(),
                subjects,
                Streaks.current(goalDays, today),
                activity.cardsReviewed(user.id(), today),
                rules.dailyGoal(),
                goalDays.contains(today));
    }

    /** Les {@code days} derniers jours, aujourd'hui compris, y compris ceux sans révision (dans l'ordre). */
    public List<DailyActivity> recentActivity(UserRef user, int days) {
        LocalDate today = LocalDate.now(clock.withZone(user.timezone()));
        LocalDate from = today.minusDays(days - 1L);
        Map<LocalDate, DailyActivity> active = activity.between(user.id(), from, today).stream()
                .collect(Collectors.toMap(DailyActivity::day, Function.identity()));
        return from.datesUntil(today.plusDays(1))
                .map(day -> active.getOrDefault(day, new DailyActivity(day, 0, false)))
                .toList();
    }

    /**
     * @param totalXp toute l'XP gagnée, bonus de l'objectif du jour compris
     * @param subjects l'XP et le niveau de chaque sujet suivi, dans l'ordre du catalogue
     * @param streak jours consécutifs avec l'objectif atteint, jusqu'à aujourd'hui (ou hier si la journée est en cours)
     * @param cardsToday cartes révisées aujourd'hui, dans le fuseau de l'utilisateur
     */
    public record Progress(
            long totalXp,
            List<SubjectXp> subjects,
            int streak,
            int cardsToday,
            int dailyGoal,
            boolean goalReachedToday) {

        public Progress {
            subjects = List.copyOf(subjects);
        }
    }

    public record SubjectXp(String subjectSlug, SubjectLevel level) {}
}
