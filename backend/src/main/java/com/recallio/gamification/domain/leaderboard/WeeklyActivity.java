package com.recallio.gamification.domain.leaderboard;

import com.recallio.gamification.domain.leaderboard.WeeklyRanking.MemberWeek;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Lecture agrégée de l'activité et de l'XP de plusieurs utilisateurs sur une semaine, pour un classement. */
public interface WeeklyActivity {

    /**
     * Un bilan par utilisateur demandé, y compris ceux qui n'ont rien fait de la semaine (tout à zéro).
     *
     * @param zone le fuseau qui borne la semaine pour l'XP ; les jours, eux, sont les jours locaux de chacun
     * @param dailyGoal cartes à réviser dans la journée pour qu'elle compte comme régulière
     */
    List<MemberWeek> of(Collection<UUID> userIds, Week week, ZoneId zone, int dailyGoal);
}
