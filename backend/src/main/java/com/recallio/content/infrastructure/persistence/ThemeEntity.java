package com.recallio.content.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "theme")
public class ThemeEntity {

    @Id
    UUID id;

    @Column(name = "subject_id", nullable = false)
    UUID subjectId;

    @Column(nullable = false, unique = true)
    String slug;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    int position;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected ThemeEntity() {}

    ThemeEntity(UUID id, UUID subjectId, String slug, String title, int position, Instant now) {
        this.id = id;
        this.subjectId = subjectId;
        this.slug = slug;
        this.title = title;
        this.position = position;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void update(String slug, String title, int position, Instant now) {
        this.slug = slug;
        this.title = title;
        this.position = position;
        this.updatedAt = now;
    }
}
