package com.recallio.ranked.infrastructure;

import com.recallio.ranked.domain.RankedRepositories.LiveQueueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaLiveQueueRepository implements LiveQueueRepository {

    private final EntityManager entityManager;

    public JpaLiveQueueRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<UUID> takeMatch(UUID userId) {
        List<?> matched = entityManager
                .createNativeQuery("""
                        delete from ranked_queue where user_id = :userId and matched_duel_id is not null
                        returning matched_duel_id
                        """, UUID.class)
                .setParameter("userId", userId)
                .getResultList();
        return matched.stream().findFirst().map(UUID.class::cast);
    }

    @Override
    public Optional<UUID> lockWaiting(String subjectSlug, UUID userId, Instant since) {
        List<?> waiting = entityManager
                .createNativeQuery("""
                        select user_id from ranked_queue
                        where subject_slug = :subject and user_id <> :userId and matched_duel_id is null
                          and heartbeat_at >= :since
                        order by heartbeat_at
                        limit 1
                        for update skip locked
                        """, UUID.class)
                .setParameter("subject", subjectSlug)
                .setParameter("userId", userId)
                .setParameter("since", since)
                .getResultList();
        return waiting.stream().findFirst().map(UUID.class::cast);
    }

    @Override
    public void match(UUID waitingUserId, UUID duelId) {
        entityManager
                .createNativeQuery("update ranked_queue set matched_duel_id = :duelId where user_id = :userId")
                .setParameter("duelId", duelId)
                .setParameter("userId", waitingUserId)
                .executeUpdate();
    }

    @Override
    public void enqueue(UUID userId, String subjectSlug, Instant now) {
        entityManager
                .createNativeQuery("""
                        insert into ranked_queue (user_id, subject_slug, heartbeat_at) values (:userId, :subject, :now)
                        on conflict (user_id) do update
                        set subject_slug = excluded.subject_slug, heartbeat_at = excluded.heartbeat_at,
                            matched_duel_id = null
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    public void leave(UUID userId) {
        entityManager
                .createNativeQuery("delete from ranked_queue where user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
