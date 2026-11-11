package com.recallio.feedback.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les signalements d'erreurs de l'utilisateur (ADR 0042). Ils restent après la suppression du compte : ils parlent d'une
 * carte, pas de leur auteur, et les effacer ferait perdre des corrections du contenu.
 */
@ApplicationScoped
public class FeedbackPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public FeedbackPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "feedback";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("content_report", sql.rowsOf("content_report", "created_at, id", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        // Rien à effacer : l'identifiant de l'auteur ne mène plus à personne (ADR 0056).
    }
}
