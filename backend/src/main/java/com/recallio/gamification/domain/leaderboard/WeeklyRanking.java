package com.recallio.gamification.domain.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Le rang de chaque membre sur la semaine (CLAUDE.md, sections 2.4 et 6.4 ; ADR 0018). La **régularité** passe avant
 * tout : les jours où l'objectif du groupe est atteint, puis l'XP (ADR 0035), puis les cartes révisées. Deux membres à
 * égalité sur ces trois critères partagent leur rang (1, 1, 3).
 */
public final class WeeklyRanking {

    private static final Comparator<MemberWeek> BEST_FIRST = Comparator.comparingInt(MemberWeek::regularDays)
            .thenComparingLong(MemberWeek::xp)
            .thenComparingInt(MemberWeek::cardsReviewed)
            .reversed();

    private WeeklyRanking() {}

    /** Les membres du meilleur au moins bon ; l'ordre d'arrivée départage l'affichage des ex æquo. */
    public static List<Standing> rank(List<MemberWeek> members) {
        List<MemberWeek> sorted = members.stream().sorted(BEST_FIRST).toList();
        List<Standing> standings = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            MemberWeek member = sorted.get(index);
            boolean tied = index > 0 && BEST_FIRST.compare(sorted.get(index - 1), member) == 0;
            int rank = tied ? standings.get(index - 1).rank() : index + 1;
            standings.add(new Standing(rank, member));
        }
        return List.copyOf(standings);
    }

    /**
     * @param regularDays jours de la semaine où le membre a révisé au moins l'objectif quotidien du groupe
     * @param xp XP gagnée dans la semaine
     */
    public record MemberWeek(UUID userId, int regularDays, long xp, int cardsReviewed) {}

    public record Standing(int rank, MemberWeek member) {}
}
