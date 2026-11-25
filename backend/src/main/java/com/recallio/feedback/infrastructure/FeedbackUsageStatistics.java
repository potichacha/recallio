package com.recallio.feedback.infrastructure;

import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * L'usage vu par le module feedback (ADR 0076) : les signalements ouverts, comptés par carte. La file des admins
 * (ADR 0042) les montre un par un ; ici, on voit les cartes qui en accumulent.
 */
@ApplicationScoped
public class FeedbackUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public FeedbackUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UUID, Long> openReports(Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return sql.countsByKey(sql.query("""
                        select card_id, count(*) from content_report
                        where resolution is null and card_id = any (cast(:cardIds as uuid[]))
                        group by card_id
                        """).setParameter("cardIds", cardIds.toArray(UUID[]::new)), UUID.class);
    }

    @Override
    public List<CardReports> mostReportedCards(int limit) {
        return sql.countsByKey(sql.query("""
                        select card_id, count(*) as open_reports from content_report
                        where resolution is null and card_id is not null
                        group by card_id
                        order by open_reports desc, card_id
                        limit :limit
                        """).setParameter("limit", limit), UUID.class).entrySet().stream()
                .map(entry -> new CardReports(entry.getKey(), entry.getValue()))
                .toList();
    }
}
