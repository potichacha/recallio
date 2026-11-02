package com.recallio.content.infrastructure.persistence;

import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Une notion du référentiel d'un sujet (ADR 0026), mappée directement : le module content reste simple. */
@Entity
@Table(name = "notion")
public class NotionEntity {

    @Id
    UUID id;

    @Column(name = "subject_id", nullable = false)
    UUID subjectId;

    @Column(nullable = false)
    String slug;

    @Column(nullable = false)
    String domain;

    @Column(nullable = false)
    String chapter;

    @Column(name = "chapter_title", nullable = false)
    String chapterTitle;

    @Column(nullable = false)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotionLevel level;

    @Column(nullable = false)
    short priority;

    /** Colonne {@code text[]} : un tableau Java est mappé tel quel par Hibernate. */
    @Column(nullable = false)
    String[] requires;

    @Column(nullable = false)
    int position;

    @Column
    @Nullable
    String lesson;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "retired_at")
    @Nullable
    Instant retiredAt;

    protected NotionEntity() {}

    NotionEntity(UUID id, UUID subjectId, ParsedNotion notion, Instant now) {
        this.id = id;
        this.subjectId = subjectId;
        this.createdAt = now;
        update(notion, now);
    }

    /** Met à jour la notion depuis sa source, et la réactive si elle avait été retirée. */
    void update(ParsedNotion notion, Instant now) {
        this.slug = notion.slug();
        this.domain = notion.domain();
        this.chapter = notion.chapter();
        this.chapterTitle = notion.chapterTitle();
        this.title = notion.title();
        this.level = notion.level();
        this.priority = (short) notion.priority();
        this.requires = notion.requires().toArray(String[]::new);
        this.position = notion.position();
        this.lesson = notion.lesson();
        this.updatedAt = now;
        this.retiredAt = null;
    }

    ParsedNotion toParsed() {
        return new ParsedNotion(
                slug, domain, chapter, chapterTitle, title, level, priority, List.of(requires), position, lesson);
    }
}
