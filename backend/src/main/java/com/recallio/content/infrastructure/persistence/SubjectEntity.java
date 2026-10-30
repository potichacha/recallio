package com.recallio.content.infrastructure.persistence;

import com.recallio.content.domain.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subject")
public class SubjectEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String slug;

    @Column(nullable = false)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Subject.Category category;

    @Column(nullable = false)
    int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Subject.Status status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected SubjectEntity() {}

    SubjectEntity(UUID id, Subject subject, Instant now) {
        this.id = id;
        this.createdAt = now;
        update(subject, now);
    }

    void update(Subject subject, Instant now) {
        this.slug = subject.slug();
        this.title = subject.title();
        this.category = subject.category();
        this.position = subject.position();
        this.status = subject.status();
        this.updatedAt = now;
    }

    Subject toDomain() {
        return new Subject(slug, title, category, position, status);
    }
}
