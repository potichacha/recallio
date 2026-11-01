package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardChoice;
import com.recallio.content.domain.CardContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

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

    /** Les choix d'un QCM, en JSON ; {@code null} pour les autres formats. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "choices")
    @Nullable
    List<CardChoice> choices;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected CardVersionEntity() {}

    CardVersionEntity(UUID id, UUID cardId, int version, CardContent content, String contentHash, Instant now) {
        this.id = id;
        this.cardId = cardId;
        this.version = version;
        this.prompt = content.prompt();
        this.answer = content.answer();
        this.choices = content.choices().isEmpty() ? null : content.choices();
        this.contentHash = contentHash;
        this.createdAt = now;
    }
}
