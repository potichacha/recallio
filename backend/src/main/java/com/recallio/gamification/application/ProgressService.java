package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.PointsLedger;
import com.recallio.gamification.domain.ScoringRules;
import com.recallio.gamification.domain.Streaks;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Cas d'usage : la progression de l'utilisateur (points, streak, objectif du jour). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ProgressService {

    private final ActivityRepository activity;
    private final PointsLedger ledger;
    private final ScoringRules rules;
    private final Clock clock;

    public ProgressService(ActivityRepository activity, PointsLedger ledger, GamificationConfig config, Clock clock) {
        this.activity = activity;
        this.ledger = ledger;
        this.rules = config.rules();
        this.clock = clock;
    }

    public Progress progress(UserRef user) {
        LocalDate today = LocalDate.now(clock.withZone(user.timezone()));
        List<LocalDate> goalDays = activity.goalDays(user.id());
        return new Progress(
                ledger.total(user.id()),
                Streaks.current(goalDays, today),
                activity.cardsReviewed(user.id(), today),
                rules.dailyGoal(),
                goalDays.contains(today));
    }

    /**
     * @param streak jours consécutifs avec l'objectif atteint, jusqu'à aujourd'hui (ou hier si la journée est en cours)
     * @param cardsToday cartes révisées aujourd'hui, dans le fuseau de l'utilisateur
     */
    public record Progress(long totalPoints, int streak, int cardsToday, int dailyGoal, boolean goalReachedToday) {}
}
