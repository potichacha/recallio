package com.recallio.review.infrastructure;

import com.recallio.review.domain.Rating;
import com.recallio.review.domain.ReviewLogEntry;
import com.recallio.review.domain.ReviewLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;

@ApplicationScoped
public class JpaReviewLogRepository implements ReviewLogRepository {

    private final EntityManager entityManager;

    public JpaReviewLogRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * {@code ON CONFLICT DO NOTHING} : si une autre transaction insère le même {@code request_id} au même moment,
     * PostgreSQL attend qu'elle se termine, puis n'insère rien. Aucune exception, aucun doublon.
     */
    @Override
    public boolean appendIfAbsent(ReviewLogEntry entry) {
        int inserted = entityManager
                .createNativeQuery("""
                        insert into review_log (id, request_id, user_id, card_id, card_version_id, rating, reviewed_at,
                                                elapsed_ms, state, stability, difficulty, due_at)
                        values (:id, :requestId, :userId, :cardId, :cardVersionId, :rating, :reviewedAt,
                                :elapsedMs, :state, :stability, :difficulty, :dueAt)
                        on conflict (request_id) do nothing
                        """)
                .setParameter("id", entry.id())
                .setParameter("requestId", entry.requestId())
                .setParameter("userId", entry.userId())
                .setParameter("cardId", entry.cardId())
                .setParameter("cardVersionId", entry.cardVersionId())
                .setParameter("rating", (short) entry.rating().value())
                .setParameter("reviewedAt", entry.reviewedAt())
                // Type explicite : un null non typé serait refusé par PostgreSQL.
                .setParameter("elapsedMs", new TypedParameterValue<>(StandardBasicTypes.INTEGER, entry.elapsedMs()))
                .setParameter("state", entry.state().name())
                .setParameter("stability", entry.stability())
                .setParameter("difficulty", entry.difficulty())
                .setParameter("dueAt", entry.dueAt())
                .executeUpdate();
        return inserted == 1;
    }

    @Override
    public Optional<ReviewLogEntry> findByRequestId(UUID requestId) {
        return entityManager
                .createQuery("select l from ReviewLogEntity l where l.requestId = :requestId", ReviewLogEntity.class)
                .setParameter("requestId", requestId)
                .getResultStream()
                .findFirst()
                .map(JpaReviewLogRepository::toDomain);
    }

    private static ReviewLogEntry toDomain(ReviewLogEntity entity) {
        return new ReviewLogEntry(
                entity.id,
                entity.requestId,
                entity.userId,
                entity.cardId,
                entity.cardVersionId,
                Rating.fromValue(entity.rating),
                entity.reviewedAt,
                entity.elapsedMs,
                entity.state,
                entity.stability,
                entity.difficulty,
                entity.dueAt);
    }
}
