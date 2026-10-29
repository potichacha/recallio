package com.recallio.content.infrastructure.persistence;

import com.recallio.content.domain.CardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "card")
public class CardEntity {

    @Id
    UUID id;

    @Column(name = "theme_id", nullable = false)
    UUID themeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CardType type;

    @Column(nullable = false)
    int position;

    @Column(name = "current_version", nullable = false)
    int currentVersion;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "retired_at")
    @Nullable
    Instant retiredAt;

    protected CardEntity() {}

    CardEntity(UUID id, UUID themeId, CardType type, int position, Instant now) {
        this.id = id;
        this.themeId = themeId;
        this.type = type;
        this.position = position;
        this.currentVersion = 1;
        this.createdAt = now;
    }
}
