package com.recallio.gamification.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** L'XP, l'activité quotidienne (le streak), les quêtes et les succès de l'utilisateur. */
@ApplicationScoped
public class GamificationPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public GamificationPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "gamification";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("points_ledger", sql.rowsOf("points_ledger", "occurred_at, id", userId));
        tables.put("daily_activity", sql.rowsOf("daily_activity", "local_date", userId));
        tables.put("quest_progress", sql.rowsOf("quest_progress", "period_start, quest", userId));
        tables.put("quest_seen", sql.rowsOf("quest_seen", "period_start, item", userId));
        tables.put("achievement", sql.rowsOf("achievement", "unlocked_at, code", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("points_ledger", userId);
        sql.deleteFrom("daily_activity", userId);
        sql.deleteFrom("quest_progress", userId);
        sql.deleteFrom("quest_seen", userId);
        sql.deleteFrom("achievement", userId);
    }
}
