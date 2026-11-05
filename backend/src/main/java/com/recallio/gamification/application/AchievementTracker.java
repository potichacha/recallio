package com.recallio.gamification.application;

import com.recallio.content.CardType;
import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.Streak;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.gamification.domain.achievement.AchievementRepository;
import com.recallio.gamification.domain.quest.QuestPeriod;
import com.recallio.review.CardReviewed;
import com.recallio.review.TourNotionValidated;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Débloque les succès (ADR 0040), une seule fois chacun. Observateur synchrone, exécuté dans la transaction de
 * l'événement. Sur une révision, il passe après {@link ActivityRecorder}, dont il lit le compte du jour.
 */
@ApplicationScoped
public class AchievementTracker {

    static final int HUNDRED_REVIEWS = 100;
    static final int THOUSAND_REVIEWS = 1000;
    static final int POLYGLOT_SUBJECTS = 3;
    static final int WEEK_STREAK = 7;
    static final int MONTH_STREAK = 30;
    static final int EXPLORER_NOTIONS = 10;

    private final AchievementRepository achievements;
    private final ActivityRepository activity;
    private final XpLedger ledger;
    private final int dailyGoal;

    public AchievementTracker(
            AchievementRepository achievements,
            ActivityRepository activity,
            XpLedger ledger,
            GamificationConfig config) {
        this.achievements = achievements;
        this.activity = activity;
        this.ledger = ledger;
        this.dailyGoal = config.rules().dailyGoal();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onCardReviewed(@Observes @Priority(Interceptor.Priority.APPLICATION + 600) CardReviewed event) {
        UUID userId = event.user().id();
        Instant at = event.reviewedAt();
        long reviews = activity.totalReviews(userId);
        unlockIf(reviews >= 1, userId, Achievement.FIRST_REVIEW, at);
        unlockIf(reviews >= HUNDRED_REVIEWS, userId, Achievement.HUNDRED_REVIEWS, at);
        unlockIf(reviews >= THOUSAND_REVIEWS, userId, Achievement.THOUSAND_REVIEWS, at);
        unlockIf(
                event.recalled() && event.cardType() == CardType.CODING_EXERCISE,
                userId,
                Achievement.FIRST_EXERCISE,
                at);
        unlockIf(ledger.totals(userId).bySubject().size() >= POLYGLOT_SUBJECTS, userId, Achievement.POLYGLOT, at);
        LocalDate day = LocalDate.ofInstant(at, event.user().timezone());
        // La série ne change qu'au moment où l'objectif du jour est atteint : inutile de la recalculer à chaque carte.
        if (activity.cardsReviewed(userId, day) == dailyGoal) {
            int streak = Streak.of(activity.goalDays(userId), day).days();
            unlockIf(streak >= WEEK_STREAK, userId, Achievement.STREAK_7, at);
            unlockIf(streak >= MONTH_STREAK, userId, Achievement.STREAK_30, at);
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onTourNotionValidated(@Observes TourNotionValidated event) {
        UUID userId = event.user().id();
        unlockIf(event.validatedNotions() >= 1, userId, Achievement.FIRST_TOUR_NOTION, event.validatedAt());
        unlockIf(event.validatedNotions() >= EXPLORER_NOTIONS, userId, Achievement.TOUR_EXPLORER, event.validatedAt());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onQuestCompleted(@Observes QuestCompleted event) {
        UUID userId = event.user().id();
        achievements.unlock(userId, Achievement.FIRST_QUEST, event.completedAt());
        unlockIf(event.quest().period() == QuestPeriod.WEEK, userId, Achievement.WEEKLY_QUEST, event.completedAt());
    }

    private void unlockIf(boolean condition, UUID userId, Achievement achievement, Instant at) {
        if (condition) {
            achievements.unlock(userId, achievement, at);
        }
    }
}
