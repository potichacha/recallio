package com.recallio.notification.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Les abonnements aux rappels de l'utilisateur (ADR 0047), sans leurs clés de chiffrement. */
@ApplicationScoped
public class NotificationPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public NotificationPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "notification";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("push_subscription", sql.rows("""
                        select id, endpoint, reminder_hour, last_reminded_on, created_at
                        from push_subscription where user_id = :userId order by created_at, id
                        """, userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("push_subscription", userId);
    }
}
