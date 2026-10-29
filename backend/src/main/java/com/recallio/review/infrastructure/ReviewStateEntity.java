package com.recallio.review.infrastructure;

import com.recallio.review.domain.LearningState;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Représentation en base de {@link com.recallio.review.domain.ReviewState}. Le domaine du module review est riche :
 * l'entité JPA en est séparée (CLAUDE.md, section 4.3), le mapping est fait par le dépôt.
 */
@Entity
@Table(name = "review_state")
public class ReviewStateEntity {

    @EmbeddedId
    Key key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LearningState state;

    @Column(name = "learning_step", nullable = false)
    int learningStep;

    @Column(nullable = false)
    double stability;

    @Column(nullable = false)
    double difficulty;

    @Column(name = "due_at", nullable = false)
    Instant dueAt;

    @Column(nullable = false)
    int reps;

    @Column(nullable = false)
    int lapses;

    @Column(name = "last_review_at", nullable = false)
    Instant lastReviewAt;

    @Column(name = "introduced_at", nullable = false, updatable = false)
    Instant introducedAt;

    protected ReviewStateEntity() {}

    ReviewStateEntity(Key key, Instant introducedAt) {
        this.key = key;
        this.introducedAt = introducedAt;
    }

    @Embeddable
    public record Key(
            @Column(name = "user_id", nullable = false) UUID userId,
            @Column(name = "card_id", nullable = false) UUID cardId)
            implements Serializable {}
}
