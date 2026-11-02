package com.recallio.live.infrastructure;

import com.recallio.live.domain.LiveGame.Standing;
import com.recallio.live.domain.LiveGameResult;
import com.recallio.live.domain.LiveResultRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Requêtes natives : un résultat s'écrit une fois, en bloc, et se relit tel quel. */
@ApplicationScoped
public class JpaLiveResultRepository implements LiveResultRepository {

    private final EntityManager entityManager;

    public JpaLiveResultRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(LiveGameResult result) {
        int inserted = entityManager
                .createNativeQuery("""
                        insert into live_game (id, group_id, subject_slug, host_id, started_at, finished_at, question_count)
                        values (:id, :groupId, :subject, :hostId, :startedAt, :finishedAt, :questionCount)
                        on conflict do nothing
                        """)
                .setParameter("id", result.gameId())
                .setParameter("groupId", result.groupId())
                .setParameter("subject", result.subjectSlug())
                .setParameter("hostId", result.hostId())
                .setParameter("startedAt", result.startedAt())
                .setParameter("finishedAt", result.finishedAt())
                .setParameter("questionCount", result.questionCount())
                .executeUpdate();
        if (inserted == 0) {
            return;
        }
        for (Standing standing : result.standings()) {
            entityManager
                    .createNativeQuery("""
                            insert into live_game_player (game_id, user_id, display_name, rank, score, correct_count)
                            values (:gameId, :userId, :displayName, :rank, :score, :correctCount)
                            """)
                    .setParameter("gameId", result.gameId())
                    .setParameter("userId", standing.userId())
                    .setParameter("displayName", standing.displayName())
                    .setParameter("rank", standing.rank())
                    .setParameter("score", standing.score())
                    .setParameter("correctCount", standing.correctCount())
                    .executeUpdate();
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<LiveGameResult> recentOfGroup(UUID groupId, int limit) {
        List<?> games = entityManager
                .createNativeQuery("""
                        select id, subject_slug, host_id, started_at, finished_at, question_count
                        from live_game
                        where group_id = :groupId
                        order by finished_at desc, id
                        limit :limit
                        """)
                .setParameter("groupId", groupId)
                .setParameter("limit", limit)
                .getResultList();
        if (games.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Standing>> standings = standingsOf(
                games.stream().map(row -> (UUID) ((Object[]) row)[0]).toList());
        return games.stream()
                .map(Object[].class::cast)
                .map(row -> new LiveGameResult(
                        (UUID) row[0],
                        groupId,
                        (String) row[1],
                        (UUID) row[2],
                        instant(row[3]),
                        instant(row[4]),
                        ((Number) row[5]).intValue(),
                        standings.getOrDefault((UUID) row[0], List.of())))
                .toList();
    }

    private Map<UUID, List<Standing>> standingsOf(List<UUID> gameIds) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select game_id, user_id, display_name, rank, score, correct_count
                        from live_game_player
                        where game_id = any(cast(:gameIds as uuid[]))
                        order by game_id, rank, display_name
                        """)
                .setParameter("gameIds", gameIds.toArray(UUID[]::new))
                .getResultList();
        Map<UUID, List<Standing>> byGame = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            byGame.computeIfAbsent((UUID) columns[0], _ -> new ArrayList<>())
                    .add(new Standing(
                            ((Number) columns[3]).intValue(),
                            (UUID) columns[1],
                            (String) columns[2],
                            ((Number) columns[4]).intValue(),
                            ((Number) columns[5]).intValue(),
                            0));
        }
        return byGame;
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
