package com.recallio.entitlement.infrastructure;

import com.recallio.platform.domain.PersonalData;
import com.recallio.platform.infrastructure.PersonalDataSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Le droit « Plus » de l'utilisateur, les codes qu'il a utilisés et ses essais ratés (ADR 0079). Tout part avec le
 * compte. Le compteur d'utilisations d'un code, lui, est sur le code et ne désigne personne : il reste, et le code ne se
 * rouvre pas.
 */
@ApplicationScoped
public class EntitlementPersonalData implements PersonalData {

    private final PersonalDataSql sql;

    public EntitlementPersonalData(PersonalDataSql sql) {
        this.sql = sql;
    }

    @Override
    public String section() {
        return "entitlement";
    }

    @Override
    public Map<String, List<Map<String, @Nullable Object>>> export(UUID userId) {
        Map<String, List<Map<String, @Nullable Object>>> tables = new LinkedHashMap<>();
        tables.put("plus_entitlement", sql.rowsOf("plus_entitlement", "user_id", userId));
        // Le code se reconnaît à ses derniers caractères ; son empreinte ne sort pas du serveur.
        tables.put("access_code_redemption", sql.rows("""
                select r.code_id, r.user_id, r.redeemed_at, r.plus_days, c.suffix as code_suffix
                from access_code_redemption r join access_code c on c.id = r.code_id
                where r.user_id = :userId
                order by r.redeemed_at, r.code_id
                """, userId));
        tables.put("access_code_attempt", sql.rowsOf("access_code_attempt", "user_id", userId));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        sql.deleteFrom("plus_entitlement", userId);
        sql.deleteFrom("access_code_redemption", userId);
        sql.deleteFrom("access_code_attempt", userId);
    }
}
