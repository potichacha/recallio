package com.recallio.ranked.infrastructure;

import static com.recallio.ranked.infrastructure.SqlValues.instant;
import static com.recallio.ranked.infrastructure.SqlValues.integer;
import static com.recallio.ranked.infrastructure.SqlValues.nullableInstant;
import static com.recallio.ranked.infrastructure.SqlValues.number;

import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.DuelAnswer;
import com.recallio.ranked.domain.Duel.DuelPlayer;
import com.recallio.ranked.domain.Duel.Mode;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.RankedRepositories.DuelRepository;
import com.recallio.ranked.domain.Season;
import com.recallio.ranked.domain.glicko.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Un duel est un petit agrégat : il se lit en entier, ses joueurs et leurs réponses avec. */
@ApplicationScoped
public class JpaDuelRepository implements DuelRepository {

    private static final String DUEL_COLUMNS =
            "select id, subject_slug, mode, status, season, created_at, expires_at, finished_at from duel";

    private final EntityManager entityManager;

    public JpaDuelRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void create(Duel duel) {
        entityManager
                .createNativeQuery("""
                        insert into duel (id, subject_slug, mode, status, season, created_at, expires_at)
                        values (:id, :subject, :mode, :status, :season, :createdAt, :expiresAt)
                        """)
                .setParameter("id", duel.id())
                .setParameter("subject", duel.subjectSlug())
                .setParameter("mode", duel.mode().name())
                .setParameter("status", duel.status().name())
                .setParameter("season", duel.season().id())
                .setParameter("createdAt", duel.createdAt())
                .setParameter("expiresAt", duel.expiresAt())
                .executeUpdate();
        for (int position = 0; position < duel.cardIds().size(); position++) {
            entityManager
                    .createNativeQuery(
                            "insert into duel_question (duel_id, position, card_id) values (:duelId, :position, :cardId)")
                    .setParameter("duelId", duel.id())
                    .setParameter("position", position)
                    .setParameter("cardId", duel.cardIds().get(position))
                    .executeUpdate();
        }
        for (DuelPlayer player : duel.players()) {
            insertPlayer(duel.id(), player.userId(), player.seat(), player.ratingBefore(), player.joinedAt());
        }
    }

    @Override
    public Optional<Duel> find(UUID id) {
        return one(DUEL_COLUMNS + " where id = :id", id);
    }

    @Override
    public Optional<Duel> lock(UUID id) {
        return one(DUEL_COLUMNS + " where id = :id for update", id);
    }

    @Override
    public Optional<Duel> unfinished(UUID userId, String subjectSlug, Mode mode, Instant now) {
        List<?> ids = entityManager
                .createNativeQuery("""
                        select d.id from duel d join duel_player p on p.duel_id = d.id
                        where p.user_id = :userId and p.finished_at is null and d.subject_slug = :subject
                          and d.mode = :mode and d.status in ('OPEN', 'FULL') and d.expires_at > :now
                        order by d.created_at desc
                        limit 1
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setParameter("mode", mode.name())
                .setParameter("now", now)
                .getResultList();
        return ids.stream().findFirst().flatMap(id -> find((UUID) id));
    }

    @Override
    public Optional<Duel> lockClosestOpen(String subjectSlug, UUID userId, double rating, Instant now) {
        List<?> ids = entityManager
                .createNativeQuery("""
                        select d.id from duel d join duel_player p on p.duel_id = d.id and p.seat = 0
                        where d.subject_slug = :subject and d.mode = 'ASYNC' and d.status = 'OPEN'
                          and d.expires_at > :now and p.user_id <> :userId
                        order by abs(p.rating_before - :rating), d.created_at
                        limit 1
                        for update of d skip locked
                        """, UUID.class)
                .setParameter("subject", subjectSlug)
                .setParameter("now", now)
                .setParameter("userId", userId)
                .setParameter("rating", rating)
                .getResultList();
        return ids.stream().findFirst().flatMap(id -> find((UUID) id));
    }

    @Override
    public void join(UUID duelId, UUID userId, Rating rating, Instant now, Instant expiresAt) {
        insertPlayer(duelId, userId, 1, rating, now);
        entityManager
                .createNativeQuery("update duel set status = 'FULL', expires_at = :expiresAt where id = :id")
                .setParameter("expiresAt", expiresAt)
                .setParameter("id", duelId)
                .executeUpdate();
    }

    @Override
    public void serve(UUID duelId, UUID userId, int position, Instant now) {
        entityManager
                .createNativeQuery("""
                        insert into duel_answer (duel_id, user_id, position, served_at, correct)
                        values (:duelId, :userId, :position, :now, false)
                        """)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .setParameter("position", position)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    public void answer(UUID duelId, UUID userId, int position, Instant now, boolean correct) {
        entityManager
                .createNativeQuery("""
                        update duel_answer set answered_at = :now, correct = :correct
                        where duel_id = :duelId and user_id = :userId and position = :position
                        """)
                .setParameter("now", now)
                .setParameter("correct", correct)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .setParameter("position", position)
                .executeUpdate();
    }

    @Override
    public void finish(UUID duelId, UUID userId, Instant now) {
        entityManager
                .createNativeQuery("""
                        update duel_player set finished_at = :now
                        where duel_id = :duelId and user_id = :userId and finished_at is null
                        """)
                .setParameter("now", now)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void settle(UUID duelId, Status status, Map<UUID, Double> ratingsAfter, Instant now) {
        entityManager
                .createNativeQuery("update duel set status = :status, finished_at = :now where id = :id")
                .setParameter("status", status.name())
                .setParameter("now", now)
                .setParameter("id", duelId)
                .executeUpdate();
        ratingsAfter.forEach((userId, rating) -> entityManager
                .createNativeQuery(
                        "update duel_player set rating_after = :rating where duel_id = :duelId and user_id = :userId")
                .setParameter("rating", rating)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .executeUpdate());
    }

    @Override
    public List<Duel> recent(UUID userId, int limit) {
        List<?> ids = entityManager
                .createNativeQuery("""
                        select d.id from duel d join duel_player p on p.duel_id = d.id
                        where p.user_id = :userId
                        order by d.created_at desc
                        """, UUID.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
        return ids.stream().map(id -> find((UUID) id).orElseThrow()).toList();
    }

    private void insertPlayer(UUID duelId, UUID userId, int seat, Rating rating, Instant joinedAt) {
        entityManager
                .createNativeQuery("""
                        insert into duel_player (duel_id, user_id, seat, rating_before, deviation_before,
                                                 volatility_before, joined_at)
                        values (:duelId, :userId, :seat, :rating, :deviation, :volatility, :joinedAt)
                        """)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .setParameter("seat", seat)
                .setParameter("rating", rating.rating())
                .setParameter("deviation", rating.deviation())
                .setParameter("volatility", rating.volatility())
                .setParameter("joinedAt", joinedAt)
                .executeUpdate();
    }

    private Optional<Duel> one(String sql, UUID id) {
        List<?> rows =
                entityManager.createNativeQuery(sql).setParameter("id", id).getResultList();
        return rows.stream().findFirst().map(row -> toDomain((Object[]) row));
    }

    private Duel toDomain(Object[] row) {
        UUID id = (UUID) row[0];
        List<?> cards = entityManager
                .createNativeQuery(
                        "select card_id from duel_question where duel_id = :id order by position", UUID.class)
                .setParameter("id", id)
                .getResultList();
        return new Duel(
                id,
                (String) row[1],
                Mode.valueOf((String) row[2]),
                Status.valueOf((String) row[3]),
                Season.parse((String) row[4]),
                cards.stream().map(UUID.class::cast).toList(),
                instant(row[5]),
                instant(row[6]),
                nullableInstant(row[7]),
                players(id));
    }

    private List<DuelPlayer> players(UUID duelId) {
        List<?> rows =
                entityManager.createNativeQuery("""
                        select user_id, seat, rating_before, deviation_before, volatility_before, joined_at,
                               finished_at, rating_after
                        from duel_player where duel_id = :id order by seat
                        """).setParameter("id", duelId).getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;
                    UUID userId = (UUID) columns[0];
                    return new DuelPlayer(
                            userId,
                            integer(columns[1]),
                            new Rating(number(columns[2]), number(columns[3]), number(columns[4])),
                            instant(columns[5]),
                            answers(duelId, userId),
                            nullableInstant(columns[6]),
                            columns[7] == null ? null : number(columns[7]));
                })
                .toList();
    }

    private List<DuelAnswer> answers(UUID duelId, UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select position, served_at, answered_at, correct from duel_answer
                        where duel_id = :duelId and user_id = :userId order by position
                        """)
                .setParameter("duelId", duelId)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;
                    return new DuelAnswer(
                            integer(columns[0]), instant(columns[1]), nullableInstant(columns[2]), (Boolean)
                                    columns[3]);
                })
                .toList();
    }
}
