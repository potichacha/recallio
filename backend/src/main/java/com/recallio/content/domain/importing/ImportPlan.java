package com.recallio.content.domain.importing;

import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.Subject;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les changements à appliquer en base pour aligner un sujet sur son document. Un plan vide signifie que le document n'a
 * pas changé : c'est ce qui rend l'import idempotent.
 *
 * @param subjectUpsert le sujet à créer ou à mettre à jour, {@code null} s'il n'a pas changé
 */
public record ImportPlan(
        @Nullable SubjectUpsert subjectUpsert,
        List<ThemeUpsert> themes,
        List<NewCard> newCards,
        List<NewVersion> newVersions,
        List<CardPlacement> placements,
        List<UUID> retiredCardIds,
        List<UUID> reactivatedCardIds,
        ImportReport report) {

    public ImportPlan {
        themes = List.copyOf(themes);
        newCards = List.copyOf(newCards);
        newVersions = List.copyOf(newVersions);
        placements = List.copyOf(placements);
        retiredCardIds = List.copyOf(retiredCardIds);
        reactivatedCardIds = List.copyOf(reactivatedCardIds);
    }

    public boolean isEmpty() {
        return subjectUpsert == null
                && themes.isEmpty()
                && newCards.isEmpty()
                && newVersions.isEmpty()
                && placements.isEmpty()
                && retiredCardIds.isEmpty()
                && reactivatedCardIds.isEmpty();
    }

    public record SubjectUpsert(UUID id, Subject subject) {}

    public record ThemeUpsert(UUID id, UUID subjectId, String slug, String title, int position) {}

    public record NewCard(UUID id, UUID themeId, int position, CardContent content, String contentHash) {}

    public record NewVersion(UUID cardId, int version, CardContent content, String contentHash) {}

    /** Thème, format ou ordre d'une carte existante qui a changé. */
    public record CardPlacement(UUID cardId, UUID themeId, CardType type, int position) {}
}
