package com.recallio.group.infrastructure;

import com.recallio.group.domain.Challenge;
import com.recallio.group.domain.ChallengeRepository;
import com.recallio.group.domain.ChallengeRun.Attempt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Requêtes natives, sans entités : un défi et ses réponses sont de simples lignes, lues et écrites en bloc. Les
 * insertions de réponses s'appuient sur la clé primaire pour rester sans effet quand elles sont rejouées.
 */
@ApplicationScoped
public class JpaChallengeRepository implements ChallengeRepository {

    private final EntityManager entityManager;

    public JpaChallengeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void create(Challenge challenge) {
        entityManager
                .createNativeQuery("""
                        insert into challenge (id, group_id, title, seconds_per_card, created_by, created_at, ends_at)
                        values (:id, :groupId, :title, :seconds, :createdBy, :createdAt, :endsAt)
                        """)
                .setParameter("id", challenge.id())
                .setParameter("groupId", challenge.groupId())
                .setParameter("title", challenge.title())
                .setParameter("seconds", (int) challenge.perCard().toSeconds())
                .setParameter("createdBy", challenge.createdBy())
                .setParameter("createdAt", challenge.createdAt())
                .setParameter("endsAt", challenge.endsAt())
                .executeUpdate();
        for (int position = 0; position < challenge.cardCount(); position++) {
            entityManager
                    .createNativeQuery("""
                            insert into challenge_card (challenge_id, position, card_id)
                            values (:challengeId, :position, :cardId)
                            """)
                    .setParameter("challengeId", challenge.id())
                    .setParameter("position", position)
                    .setParameter("cardId", challenge.cardIds().get(position))
                    .executeUpdate();
        }
    }

    @Override
    public Optional<Challenge> findById(UUID challengeId) {
        return challenges("where c.id = :key", challengeId, 1).stream().findFirst();
    }

    @Override
    public List<Challenge> findByGroup(UUID groupId, int limit) {
        return challenges("where c.group_id = :key", groupId, limit);
    }

    @Override
    public List<Attempt> attempts(UUID challengeId, UUID userId) {
        return attempts("and user_id = :userId", challengeId, Map.of("userId", userId))
                .getOrDefault(userId, List.of());
    }

    @Override
    public Map<UUID, List<Attempt>> attemptsByUser(UUID challengeId) {
        return attempts("", challengeId, Map.of());
    }

    @Override
    public void present(UUID challengeId, UUID userId, int position, Instant presentedAt) {
        entityManager
                .createNativeQuery("""
                        insert into challenge_answer (challenge_id, user_id, position, presented_at)
                        values (:challengeId, :userId, :position, :presentedAt)
                        on conflict do nothing
                        """)
                .setParameter("challengeId", challengeId)
                .setParameter("userId", userId)
                .setParameter("position", position)
                .setParameter("presentedAt", presentedAt)
                .executeUpdate();
    }

    @Override
    public boolean answer(
            UUID challengeId, UUID userId, int position, Instant answeredAt, boolean correct, boolean timedOut) {
        return entityManager
                        .createNativeQuery("""
                                update challenge_answer
                                set answered_at = :answeredAt, correct = :correct, timed_out = :timedOut
                                where challenge_id = :challengeId and user_id = :userId and position = :position
                                  and answered_at is null
                                """)
                        .setParameter("answeredAt", answeredAt)
                        .setParameter("correct", correct)
                        .setParameter("timedOut", timedOut)
                        .setParameter("challengeId", challengeId)
                        .setParameter("userId", userId)
                        .setParameter("position", position)
                        .executeUpdate()
                == 1;
    }

    /** Les défis répondant à la condition, avec leur série (agrégée dans l'ordre des positions). */
    private List<Challenge> challenges(String condition, UUID key, int limit) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select c.id, c.group_id, c.title, c.seconds_per_card, c.created_by, c.created_at, c.ends_at,
                               array(select cc.card_id from challenge_card cc
                                     where cc.challenge_id = c.id order by cc.position)
                        from challenge c
                        %s
                        order by c.created_at desc, c.id
                        limit :limit
                        """.formatted(condition))
                .setParameter("key", key)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(JpaChallengeRepository::toChallenge)
                .toList();
    }

    private Map<UUID, List<Attempt>> attempts(String condition, UUID challengeId, Map<String, Object> parameters) {
        var query = entityManager.createNativeQuery("""
                        select user_id, position, presented_at, answered_at, correct, timed_out
                        from challenge_answer
                        where challenge_id = :challengeId %s
                        order by user_id, position
                        """.formatted(condition)).setParameter("challengeId", challengeId);
        parameters.forEach(query::setParameter);
        List<?> rows = query.getResultList();
        Map<UUID, List<Attempt>> byUser = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            byUser.computeIfAbsent((UUID) columns[0], _ -> new ArrayList<>())
                    .add(new Attempt(
                            ((Number) columns[1]).intValue(),
                            instant(columns[2]),
                            columns[3] == null ? null : instant(columns[3]),
                            (Boolean) columns[4],
                            (Boolean) columns[5]));
        }
        return byUser;
    }

    private static Challenge toChallenge(Object[] row) {
        return new Challenge(
                (UUID) row[0],
                (UUID) row[1],
                (String) row[2],
                Arrays.stream((Object[]) row[7]).map(UUID.class::cast).toList(),
                Duration.ofSeconds(((Number) row[3]).longValue()),
                (UUID) row[4],
                instant(row[5]),
                instant(row[6]));
    }

    /** Selon le pilote, une colonne {@code timestamptz} arrive en {@link Instant}, {@link OffsetDateTime} ou {@link Timestamp}. */
    private static Instant instant(Object value) {
        return switch (value) {
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> (Instant) value;
        };
    }
}
