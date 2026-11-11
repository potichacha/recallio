package com.recallio.user.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Le profil de l'utilisateur, ses sujets suivis et son profil public. Aucune table d'un autre module ne référence
 * {@code app_user} par une clé étrangère : l'ordre dans lequel les modules effacent leurs données n'importe pas.
 */
@ApplicationScoped
public class UserPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public UserPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "user";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("app_user", sql.rows("select * from app_user where id = :userId", userId));
        tables.put("followed_subject", sql.rowsOf("followed_subject", "followed_at, subject_slug", userId));
        tables.put("public_profile", sql.rowsOf("public_profile", "handle", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("public_profile", userId);
        // Les sujets suivis partent avec le profil (ON DELETE CASCADE).
        sql.execute("delete from app_user where id = :userId", userId);
    }
}
