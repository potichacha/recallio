package com.recallio.gamification.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * L'usage vu par le module gamification (ADR 0076) : l'activité de chaque jour et les quêtes terminées.
 *
 * <p>{@code daily_activity} porte le jour de chacun dans son fuseau, pas un instant : la période y devient des jours du
 * calendrier UTC. Un jour local peut déborder d'un jour au bord de la fenêtre, comme pour la rétention (ADR 0050).
 */
@ApplicationScoped
public class GamificationUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public GamificationUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(UsageFeature.QUEST, sql.usersBetween("""
                select distinct user_id from quest_progress
                where completed_at >= :from and completed_at < :to
                """, from, to));
    }

    @Override
    public Map<DailyCount, Map<LocalDate, Long>> dailyCounts(Instant from, Instant to) {
        Map<DailyCount, Map<LocalDate, Long>> counts = new EnumMap<>(DailyCount.class);
        counts.put(DailyCount.REVIEWERS, new TreeMap<>());
        counts.put(DailyCount.REVIEWS, new TreeMap<>());
        counts.put(DailyCount.GOALS_REACHED, new TreeMap<>());
        var rows = sql.query("""
                        select local_date,
                               count(*),
                               sum(cards_reviewed),
                               count(*) filter (where goal_reached)
                        from daily_activity
                        where local_date >= :first and local_date < :end and cards_reviewed > 0
                        group by local_date
                        """)
                .setParameter("first", LocalDate.ofInstant(from, ZoneOffset.UTC))
                .setParameter("end", LocalDate.ofInstant(to, ZoneOffset.UTC))
                .getResultList();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            LocalDate day = UsageStatisticsSql.toLocalDate(columns[0]);
            counts.get(DailyCount.REVIEWERS).put(day, ((Number) columns[1]).longValue());
            counts.get(DailyCount.REVIEWS).put(day, ((Number) columns[2]).longValue());
            counts.get(DailyCount.GOALS_REACHED).put(day, ((Number) columns[3]).longValue());
        }
        return counts;
    }
}
