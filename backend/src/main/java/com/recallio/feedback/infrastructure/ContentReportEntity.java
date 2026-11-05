package com.recallio.feedback.infrastructure;

import com.recallio.feedback.domain.ReportReason;
import com.recallio.feedback.domain.ReportResolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Un module simple : l'entité reste à l'infrastructure, le domaine a son record (CLAUDE.md, section 4.3). */
@Entity
@Table(name = "content_report")
public class ContentReportEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "subject_slug", nullable = false)
    String subjectSlug;

    @Column(name = "card_id")
    UUID cardId;

    @Column(name = "notion_slug")
    String notionSlug;

    Integer page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ReportReason reason;

    @Column(nullable = false)
    String comment;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Enumerated(EnumType.STRING)
    ReportResolution resolution;

    @Column(name = "resolved_at")
    Instant resolvedAt;

    protected ContentReportEntity() {}
}
