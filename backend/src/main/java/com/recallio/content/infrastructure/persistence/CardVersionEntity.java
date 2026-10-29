package com.recallio.content.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Une version immuable du texte d'une carte : on n'en modifie jamais une, on en ajoute une nouvelle. */
@Entity
@Table(name = "card_version")
public class CardVersionEntity {

    @Id
    UUID id;

    @Column(name = "card_id", nullable = false)
    UUID cardId;

    @Column(nullable = false)
    int version;

    @Column(nullable = false)
    String prompt;

    @Column(nullable = false)
    String answer;

    @Column(name = "content_hash", nullable = false)
    String contentHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected CardVersionEntity() {}

    CardVersionEntity(
            UUID id, UUID cardId, int version, String prompt, String answer, String contentHash, Instant now) {
        this.id = id;
        this.cardId = cardId;
        this.version = version;
        this.prompt = prompt;
        this.answer = answer;
        this.contentHash = contentHash;
        this.createdAt = now;
    }
}
