package com.recallio.gamification;

import com.recallio.user.UserRef;

/** API publique du module gamification (ADR 0001) : où en est l'utilisateur de son objectif du jour, pour les rappels. */
public interface DailyGoals {

    DailyStatus status(UserRef user);

    /**
     * @param streak les jours consécutifs avec l'objectif atteint (ADR 0036)
     * @param goalReachedToday vrai si l'objectif est déjà atteint aujourd'hui, dans le fuseau de l'utilisateur
     */
    record DailyStatus(int streak, boolean goalReachedToday) {}
}
