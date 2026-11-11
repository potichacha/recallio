package com.recallio.live.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les parties live terminées de l'utilisateur (ADR 0023). Le résultat d'une partie est celui de tous ses joueurs : il
 * reste, mais le nom enregistré avec lui est remplacé.
 */
@ApplicationScoped
public class LivePersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public LivePersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "live";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("live_game_player", sql.rowsOf("live_game_player", "game_id", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.execute(
                "update live_game_player set display_name = '" + DELETED_ACCOUNT_NAME + "' where user_id = :userId",
                userId);
    }
}
