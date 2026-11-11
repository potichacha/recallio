package com.recallio.platform.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les requêtes des adaptateurs {@code PersonalData} : lire les lignes d'un utilisateur comme elles sont en base, ou les
 * effacer. Les noms de table et de colonne viennent du code, jamais d'une requête.
 */
@ApplicationScoped
public class PersonalDataSql {

    private static final String USER_ID = "userId";

    private final EntityManager entityManager;

    public PersonalDataSql(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Toutes les colonnes des lignes de {@code table} dont {@code user_id} est l'utilisateur. */
    public List<Map<String, @Nullable Object>> rowsOf(String table, String orderBy, UUID userId) {
        return rows("select * from " + table + " where user_id = :userId order by " + orderBy, userId);
    }

    /** Le résultat d'une requête dont le seul paramètre est {@code :userId}, une ligne par map, clés dans l'ordre. */
    public List<Map<String, @Nullable Object>> rows(String sql, UUID userId) {
        List<Tuple> tuples = entityManager
                .createNativeQuery(sql, Tuple.class)
                .setParameter(USER_ID, userId)
                .getResultList();
        return tuples.stream().map(PersonalDataSql::row).toList();
    }

    /** Efface les lignes de {@code table} dont {@code user_id} est l'utilisateur. */
    public void deleteFrom(String table, UUID userId) {
        execute("delete from " + table + " where user_id = :userId", userId);
    }

    /** Exécute une modification dont le seul paramètre est {@code :userId}. */
    public void execute(String sql, UUID userId) {
        entityManager.createNativeQuery(sql).setParameter(USER_ID, userId).executeUpdate();
    }

    private static Map<String, @Nullable Object> row(Tuple tuple) {
        Map<String, @Nullable Object> row = new LinkedHashMap<>();
        for (TupleElement<?> element : tuple.getElements()) {
            row.put(element.getAlias(), tuple.get(element));
        }
        return row;
    }
}
