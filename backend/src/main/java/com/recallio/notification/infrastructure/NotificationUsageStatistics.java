package com.recallio.notification.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'usage vu par le module notification (ADR 0076) : les comptes qui ont accepté les rappels sur au moins un appareil,
 * à ce jour. Un abonnement coupé est effacé (ADR 0047) : rien ne dit qui en a eu un.
 */
@ApplicationScoped
public class NotificationUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public NotificationUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(UsageFeature.REMINDERS, sql.users("select distinct user_id from push_subscription"));
    }
}
