package com.recallio.platform.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Les requêtes des adaptateurs {@code UsageStatistics} (ADR 0076) : des comptes distincts, ou des comptes par jour ou
 * par clé, toujours agrégés en base. Le texte des requêtes vient du code ; une période se lie par {@code :from}
 * (inclus) et {@code :to} (exclu).
 */
@ApplicationScoped
public class UsageStatisticsSql {

    private static final String FROM = "from";
    private static final String TO = "to";

    private final EntityManager entityManager;

    public UsageStatisticsSql(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Les utilisateurs que rend une requête d'une seule colonne, sans paramètre. */
    public Set<UUID> users(String sql) {
        return users(entityManager.createNativeQuery(sql, UUID.class));
    }

    /** Les utilisateurs que rend une requête d'une seule colonne, sur une période. */
    public Set<UUID> usersBetween(String sql, Instant from, Instant to) {
        return users(between(sql, from, to));
    }

    /** Les utilisateurs que rend une requête d'une seule colonne, dont les paramètres sont déjà liés. */
    public Set<UUID> users(Query query) {
        Set<UUID> users = new HashSet<>();
        for (Object row : query.getResultList()) {
            users.add((UUID) row);
        }
        return users;
    }

    /** Le résultat d'une requête {@code (jour, compte)} sur une période, dans l'ordre des jours. */
    public Map<LocalDate, Long> countsByDay(String sql, Instant from, Instant to) {
        Map<LocalDate, Long> counts = new TreeMap<>();
        for (Object row : between(sql, from, to).getResultList()) {
            Object[] columns = (Object[]) row;
            counts.put(toLocalDate(columns[0]), ((Number) columns[1]).longValue());
        }
        return counts;
    }

    /** Le résultat d'une requête {@code (clé, compte)} sans paramètre, dans l'ordre de la requête. */
    public <K> Map<K, Long> countsByKey(String sql, Class<K> keyType) {
        return countsByKey(entityManager.createNativeQuery(sql), keyType);
    }

    /** Le résultat d'une requête {@code (clé, compte)} dont les paramètres sont déjà liés, dans son ordre. */
    public <K> Map<K, Long> countsByKey(Query query, Class<K> keyType) {
        Map<K, Long> counts = new LinkedHashMap<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            counts.put(keyType.cast(columns[0]), ((Number) columns[1]).longValue());
        }
        return counts;
    }

    /** Une requête native liée à une période, pour y lier d'autres paramètres ou la lire ligne par ligne. */
    public Query between(String sql, Instant from, Instant to) {
        return entityManager.createNativeQuery(sql).setParameter(FROM, from).setParameter(TO, to);
    }

    /** Une requête native sans période. */
    public Query query(String sql) {
        return entityManager.createNativeQuery(sql);
    }

    /** Selon le pilote, une colonne {@code date} arrive en {@link LocalDate} ou en {@link java.sql.Date}. */
    public static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
}
