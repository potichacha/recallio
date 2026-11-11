package com.recallio.ranked.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Le classé de l'utilisateur (ADR 0046). Sa cote, ses insignes et sa place dans la file s'effacent ; ses duels joués
 * restent, parce qu'ils sont aussi ceux de ses adversaires.
 */
@ApplicationScoped
public class RankedPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public RankedPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "ranked";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("ranked_standing", sql.rowsOf("ranked_standing", "subject_slug", userId));
        tables.put("ranked_badge", sql.rowsOf("ranked_badge", "season, subject_slug", userId));
        tables.put("duel_player", sql.rowsOf("duel_player", "joined_at, duel_id", userId));
        tables.put("duel_answer", sql.rowsOf("duel_answer", "served_at, duel_id, position", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("ranked_standing", userId);
        sql.deleteFrom("ranked_badge", userId);
        sql.deleteFrom("ranked_queue", userId);
        // Un duel que personne n'a encore rejoint n'a plus lieu d'être ; ses questions et ses réponses partent avec
        // lui.
        sql.execute("""
                delete from duel d
                where d.status = 'OPEN'
                  and exists (select 1 from duel_player p where p.duel_id = d.id and p.user_id = :userId)
                """, userId);
    }
}
