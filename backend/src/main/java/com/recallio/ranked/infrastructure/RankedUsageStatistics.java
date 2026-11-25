package com.recallio.ranked.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** L'usage vu par le module ranked (ADR 0076) : les joueurs qui ont ouvert ou rejoint un duel sur la période. */
@ApplicationScoped
public class RankedUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public RankedUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(UsageFeature.RANKED_DUEL, sql.usersBetween("""
                select distinct user_id from duel_player
                where joined_at >= :from and joined_at < :to
                """, from, to));
    }
}
