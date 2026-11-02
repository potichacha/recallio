package com.recallio.content.domain.importing;

import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les changements à appliquer en base pour aligner un sujet sur sa source. Un plan vide signifie que la source n'a pas
 * changé : c'est ce qui rend l'import idempotent.
 *
 * @param subjectUpsert le sujet à créer ou à mettre à jour, {@code null} s'il n'a pas changé
 * @param retiredThemeIds les thèmes du sujet qui ont disparu de sa source : retirés, ce qui libère leur slug
 */
public record ImportPlan(
        @Nullable SubjectUpsert subjectUpsert,
        List<ThemeUpsert> themes,
        List<UUID> retiredThemeIds,
        List<NotionUpsert> notions,
        List<UUID> retiredNotionIds,
        List<NewCard> newCards,
        List<NewVersion> newVersions,
        List<CardPlacement> placements,
        List<UUID> retiredCardIds,
        List<UUID> reactivatedCardIds,
        ImportReport report) {

    public ImportPlan {
        themes = List.copyOf(themes);
        retiredThemeIds = List.copyOf(retiredThemeIds);
        notions = List.copyOf(notions);
        retiredNotionIds = List.copyOf(retiredNotionIds);
        newCards = List.copyOf(newCards);
        newVersions = List.copyOf(newVersions);
        placements = List.copyOf(placements);
        retiredCardIds = List.copyOf(retiredCardIds);
        reactivatedCardIds = List.copyOf(reactivatedCardIds);
    }

    public boolean isEmpty() {
        return subjectUpsert == null
                && themes.isEmpty()
                && retiredThemeIds.isEmpty()
                && notions.isEmpty()
                && retiredNotionIds.isEmpty()
                && newCards.isEmpty()
                && newVersions.isEmpty()
                && placements.isEmpty()
                && retiredCardIds.isEmpty()
                && reactivatedCardIds.isEmpty();
    }

    public record SubjectUpsert(UUID id, Subject subject) {}

    /** Crée, met à jour ou réactive un thème. */
    public record ThemeUpsert(UUID id, UUID subjectId, String slug, String title, int position) {}

    /** Crée, met à jour ou réactive une notion. */
    public record NotionUpsert(UUID id, UUID subjectId, ParsedNotion notion) {}

    /** @param notionId la notion de la carte, {@code null} pour une carte d'un document Markdown */
    public record NewCard(
            UUID id, UUID themeId, @Nullable UUID notionId, int position, CardContent content, String contentHash) {}

    public record NewVersion(UUID cardId, int version, CardContent content, String contentHash) {}

    /** Thème, notion, format ou ordre d'une carte existante qui a changé. */
    public record CardPlacement(
            UUID cardId, UUID themeId, @Nullable UUID notionId, CardType type, int position) {}
}
