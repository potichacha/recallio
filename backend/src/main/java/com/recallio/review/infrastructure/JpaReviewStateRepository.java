package com.recallio.review.infrastructure;

import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaReviewStateRepository implements ReviewStateRepository {

    private final EntityManager entityManager;

    public JpaReviewStateRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<UUID, ReviewState> findAllByUser(UUID userId) {
        return entityManager
                .createQuery("select s from ReviewStateEntity s where s.key.userId = :userId", ReviewStateEntity.class)
                .setParameter("userId", userId)
                .getResultStream()
                .collect(Collectors.toMap(entity -> entity.key.cardId(), JpaReviewStateRepository::toDomain));
    }

    @Override
    public Optional<ReviewState> findForUpdate(UUID userId, UUID cardId) {
        return Optional.ofNullable(entityManager.find(
                        ReviewStateEntity.class,
                        new ReviewStateEntity.Key(userId, cardId),
                        LockModeType.PESSIMISTIC_WRITE))
                .map(JpaReviewStateRepository::toDomain);
    }

    @Override
    public void save(UUID userId, UUID cardId, ReviewState state) {
        var key = new ReviewStateEntity.Key(userId, cardId);
        Instant lastReview = Objects.requireNonNull(state.lastReviewAt(), "Seule une carte révisée est enregistrée");
        ReviewStateEntity existing = entityManager.find(ReviewStateEntity.class, key);
        ReviewStateEntity entity = existing != null ? existing : new ReviewStateEntity(key, lastReview);
        copyInto(entity, state, lastReview);
        // Les valeurs d'une nouvelle entité sont lues au moment de persist : elle doit être remplie avant.
        if (existing == null) {
            entityManager.persist(entity);
        }
    }

    private static void copyInto(ReviewStateEntity entity, ReviewState state, Instant lastReview) {
        entity.state = state.state();
        entity.learningStep = state.step();
        entity.stability = state.stability();
        entity.difficulty = state.difficulty();
        entity.dueAt = state.dueAt();
        entity.reps = state.reps();
        entity.lapses = state.lapses();
        entity.lastReviewAt = lastReview;
    }

    @Override
    public long countIntroducedSince(UUID userId, Instant since) {
        return entityManager
                .createQuery(
                        "select count(s) from ReviewStateEntity s where s.key.userId = :userId and s.introducedAt >= :since",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("since", since)
                .getSingleResult();
    }

    private static ReviewState toDomain(ReviewStateEntity entity) {
        return new ReviewState(
                entity.state,
                entity.learningStep,
                entity.stability,
                entity.difficulty,
                entity.dueAt,
                entity.reps,
                entity.lapses,
                entity.lastReviewAt);
    }
}
