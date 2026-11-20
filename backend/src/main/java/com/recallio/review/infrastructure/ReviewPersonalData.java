package com.recallio.review.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * La progression de l'utilisateur : sa mémoire de chaque carte, son journal de révision, ses examens blancs, son Tour,
 * ses notions connues au test de niveau (ADR 0068), les dates de ses entretiens (ADR 0066) et ses réponses aux défis du
 * jour (ADR 0067). Les défis eux-mêmes, les questions du jour, ne sont à personne : ils restent.
 */
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
        tables.put("known_notion", sql.rowsOf("known_notion", "known_at, notion_id", userId));
        tables.put("interview_plan", sql.rowsOf("interview_plan", "subject_slug", userId));
        tables.put(
                "daily_challenge_answer",
                sql.rowsOf("daily_challenge_answer", "answered_at, challenge_id, position", userId));
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
        sql.deleteFrom("known_notion", userId);
        sql.deleteFrom("interview_plan", userId);
        // Les chiffres du jour, agrégés et sans nom, ne comptent plus ses réponses.
        sql.deleteFrom("daily_challenge_answer", userId);
    }
}
