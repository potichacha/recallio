package com.recallio.entitlement.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'usage vu par le module entitlement (ADR 0076, ADR 0079) : les comptes qui ont Plus à ce jour, et ceux qui ont
 * utilisé un code sur la période.
 */
@ApplicationScoped
public class EntitlementUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;
    private final Clock clock;

    public EntitlementUsageStatistics(UsageStatisticsSql sql, Clock clock) {
        this.sql = sql;
        this.clock = clock;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        Map<UsageFeature, Set<UUID>> users = new EnumMap<>(UsageFeature.class);
        // Un droit n'a pas d'histoire : sa ligne dit quand il finit, pas quand il a servi.
        users.put(
                UsageFeature.PLUS,
                sql.users(sql.query("select user_id from plus_entitlement where ends_at > :now")
                        .setParameter("now", clock.instant())));
        users.put(UsageFeature.ACCESS_CODE, sql.usersBetween("""
                select distinct user_id from access_code_redemption
                where redeemed_at >= :from and redeemed_at < :to
                """, from, to));
        return users;
    }
}
