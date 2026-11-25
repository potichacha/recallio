package com.recallio.live.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'usage vu par le module live (ADR 0076) : les joueurs des parties finies sur la période. Une partie abandonnée ne
 * laisse aucune ligne (ADR 0023) : elle ne se compte pas.
 */
@ApplicationScoped
public class LiveUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public LiveUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(UsageFeature.LIVE_GAME, sql.usersBetween("""
                select distinct player.user_id
                from live_game_player player
                join live_game game on game.id = player.game_id
                where game.finished_at >= :from and game.finished_at < :to
                """, from, to));
    }
}
