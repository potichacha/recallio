package com.recallio.review.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** La progression de l'utilisateur : sa mémoire de chaque carte, son journal de révision, ses examens blancs, son Tour. */
@ApplicationScoped
public class ReviewPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public ReviewPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "review";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("review_state", sql.rowsOf("review_state", "card_id", userId));
        tables.put("review_log", sql.rowsOf("review_log", "reviewed_at, id", userId));
        tables.put("mock_exam", sql.rowsOf("mock_exam", "started_at, id", userId));
        tables.put("tour_progress", sql.rowsOf("tour_progress", "validated_at, notion_id", userId));
        tables.put("tour_last_page", sql.rowsOf("tour_last_page", "subject_slug", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("review_state", userId);
        sql.deleteFrom("review_log", userId);
        // Les questions d'un examen partent avec lui (ON DELETE CASCADE).
        sql.deleteFrom("mock_exam", userId);
        sql.deleteFrom("tour_progress", userId);
        sql.deleteFrom("tour_last_page", userId);
    }
}
