package com.recallio.notification.application;

import com.recallio.gamification.DailyGoals;
import com.recallio.gamification.DailyGoals.DailyStatus;
import com.recallio.notification.domain.PushPorts.PushSubscriptionRepository;
import com.recallio.notification.domain.PushSubscription;
import com.recallio.notification.domain.Reminder;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Le choix des rappels dus (ADR 0047), en base : chaque rappel est réservé pour son jour avant d'être envoyé, pour que
 * deux instances n'envoient pas deux fois le même.
 */
@ApplicationScoped
public class ReminderPlanner {

    private final PushSubscriptionRepository subscriptions;
    private final UserDirectory users;
    private final DailyGoals goals;

    public ReminderPlanner(PushSubscriptionRepository subscriptions, UserDirectory users, DailyGoals goals) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.goals = goals;
    }

    /** Réserve les rappels dus à cet instant, et les rend avec leur message. */
    @Transactional
    public List<Due> claimDue(Instant now) {
        List<Due> due = new ArrayList<>();
        for (PushSubscription subscription : subscriptions.all()) {
            Optional<UserRef> user = users.user(subscription.userId());
            if (user.isEmpty()) {
                continue;
            }
            ZonedDateTime local = now.atZone(user.get().timezone());
            if (!Reminder.isDue(subscription.reminderHour(), local, subscription.lastRemindedOn())) {
                continue;
            }
            DailyStatus status = goals.status(user.get());
            if (!status.goalReachedToday() && subscriptions.claim(subscription.id(), local.toLocalDate())) {
                due.add(new Due(subscription, Reminder.forStreak(status.streak())));
            }
        }
        return due;
    }

    /** L'appareil s'est désabonné auprès de son navigateur : l'abonnement est oublié. */
    @Transactional
    public void forget(PushSubscription subscription) {
        subscriptions.delete(subscription.id());
    }

    public record Due(PushSubscription subscription, Reminder reminder) {}
}
