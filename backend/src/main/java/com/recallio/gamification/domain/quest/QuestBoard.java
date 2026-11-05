package com.recallio.gamification.domain.quest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Les quêtes d'une période (ADR 0040) : trois par jour, deux par semaine, prises dans le catalogue. Le choix ne se
 * stocke pas : il se recalcule, toujours le même pour un utilisateur et une période, et il tourne d'une période à
 * l'autre, avec un décalage propre à chaque utilisateur.
 */
public final class QuestBoard {

    static final int DAILY_QUESTS = 3;
    static final int WEEKLY_QUESTS = 2;

    private QuestBoard() {}

    /** Les quêtes de la période de ce type qui contient {@code day}. */
    public static List<Quest> of(UUID userId, QuestPeriod period, LocalDate day) {
        List<Quest> pool = Arrays.stream(Quest.values())
                .filter(quest -> quest.period() == period)
                .toList();
        int count = period == QuestPeriod.DAY ? DAILY_QUESTS : WEEKLY_QUESTS;
        long periodIndex =
                period == QuestPeriod.DAY ? day.toEpochDay() : period.start(day).toEpochDay() / 7;
        int offset = (int) Math.floorMod(periodIndex + userId.hashCode(), (long) pool.size());
        List<Quest> chosen = new ArrayList<>();
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            chosen.add(pool.get((offset + i) % pool.size()));
        }
        return List.copyOf(chosen);
    }
}
