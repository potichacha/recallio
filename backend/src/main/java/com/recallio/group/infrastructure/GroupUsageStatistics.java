package com.recallio.group.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'usage vu par le module group (ADR 0076) : les membres d'au moins un groupe, à ce jour, et ceux à qui un défi de
 * groupe a présenté une carte sur la période.
 */
@ApplicationScoped
public class GroupUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public GroupUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(
                UsageFeature.GROUP,
                sql.users("select distinct user_id from group_member"),
                UsageFeature.GROUP_CHALLENGE,
                sql.usersBetween("""
                        select distinct user_id from challenge_answer
                        where presented_at >= :from and presented_at < :to
                        """, from, to));
    }
}
