package com.recallio.review.infrastructure;

import com.recallio.review.domain.LearningState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** Lecture du journal de révision. Les insertions passent par une requête native (voir {@link JpaReviewLogRepository}). */
@Entity
@Immutable
@Table(name = "review_log")
public class ReviewLogEntity {

    @Id
    UUID id;

    @Column(name = "request_id", nullable = false)
    UUID requestId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "card_id", nullable = false)
    UUID cardId;

    @Column(name = "card_version_id", nullable = false)
    UUID cardVersionId;

    @Column(nullable = false)
    short rating;

    @Column(name = "reviewed_at", nullable = false)
    Instant reviewedAt;

    @Column(name = "elapsed_ms")
    @Nullable
    Integer elapsedMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LearningState state;

    @Column(nullable = false)
    double stability;

    @Column(nullable = false)
    double difficulty;

    @Column(name = "due_at", nullable = false)
    Instant dueAt;

    protected ReviewLogEntity() {}
}
