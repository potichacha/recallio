package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.leaderboard.Week;
import com.recallio.gamification.domain.leaderboard.WeeklyActivity;
import com.recallio.gamification.domain.leaderboard.WeeklyRanking.MemberWeek;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Une requête pour tout le groupe : chaque membre demandé (même inactif) avec ses jours réguliers, ses cartes et son XP
 * de la semaine. Les deux agrégats sont calculés séparément puis joints, pour ne pas multiplier les lignes.
 */
@ApplicationScoped
public class JpaWeeklyActivity implements WeeklyActivity {

    private final EntityManager entityManager;

    public JpaWeeklyActivity(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<MemberWeek> of(Collection<UUID> userIds, Week week, ZoneId zone, int dailyGoal) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        with member as (select unnest(cast(:userIds as uuid[])) as user_id),
                        days as (
                            select user_id,
                                   count(*) filter (where cards_reviewed >= :goal) as regular_days,
                                   sum(cards_reviewed) as cards
                            from daily_activity
                            where user_id in (select user_id from member) and local_date between :monday and :sunday
                            group by user_id),
                        xp as (
                            select user_id, sum(amount) as xp
                            from points_ledger
                            where user_id in (select user_id from member) and occurred_at >= :start and occurred_at < :end
                            group by user_id)
                        select member.user_id,
                               coalesce(days.regular_days, 0),
                               coalesce(xp.xp, 0),
                               coalesce(days.cards, 0)
                        from member
                        left join days on days.user_id = member.user_id
                        left join xp on xp.user_id = member.user_id
                        """)
                .setParameter("userIds", userIds.toArray(UUID[]::new))
                .setParameter("goal", dailyGoal)
                .setParameter("monday", week.monday())
                .setParameter("sunday", week.sunday())
                .setParameter("start", week.startIn(zone))
                .setParameter("end", week.endIn(zone))
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new MemberWeek(
                        (UUID) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).intValue()))
                .toList();
    }
}
