package com.recallio.gamification.domain.quest;

/**
 * Les quêtes (ADR 0040). Elles font varier les formats et récompensent le Tour, jamais la vitesse : aucune ne dépend du
 * temps passé sur une carte. Stockées par leur nom : ne pas renommer une valeur existante.
 *
 * @param target ce qu'il faut atteindre pour terminer la quête
 * @param xp l'XP qu'elle rapporte, une fois terminée
 */
public enum Quest {
    DAILY_REVIEWS(QuestPeriod.DAY, QuestEvent.REVIEW, 15, 20),
    DAILY_VERIFIED(QuestPeriod.DAY, QuestEvent.VERIFIED_SUCCESS, 5, 25),
    DAILY_CODE(QuestPeriod.DAY, QuestEvent.CODE_CARD, 3, 25),
    DAILY_TOUR_PAGES(QuestPeriod.DAY, QuestEvent.TOUR_PAGE, 3, 15),
    DAILY_TOUR_NOTION(QuestPeriod.DAY, QuestEvent.TOUR_NOTION, 1, 20),
    WEEKLY_DAYS(QuestPeriod.WEEK, QuestEvent.ACTIVE_DAY, 5, 60),
    WEEKLY_EXERCISES(QuestPeriod.WEEK, QuestEvent.EXERCISE_PASSED, 2, 60),
    WEEKLY_FORMATS(QuestPeriod.WEEK, QuestEvent.NEW_FORMAT, 5, 50),
    WEEKLY_TOUR(QuestPeriod.WEEK, QuestEvent.TOUR_NOTION, 5, 60);

    private final QuestPeriod period;
    private final QuestEvent event;
    private final int target;
    private final int xp;

    Quest(QuestPeriod period, QuestEvent event, int target, int xp) {
        this.period = period;
        this.event = event;
        this.target = target;
        this.xp = xp;
    }

    public QuestPeriod period() {
        return period;
    }

    public QuestEvent event() {
        return event;
    }

    public int target() {
        return target;
    }

    public int xp() {
        return xp;
    }
}
