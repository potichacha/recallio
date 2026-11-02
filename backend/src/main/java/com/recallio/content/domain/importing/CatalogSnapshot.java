package com.recallio.content.domain.importing;

import com.recallio.content.CardType;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * État d'un sujet en base avant son import : ce à quoi on compare sa source. Seuls les thèmes, notions et cartes de ce
 * sujet en font partie : importer un sujet ne touche jamais aux autres.
 *
 * @param subject le sujet tel qu'il est en base, {@code null} s'il n'a jamais été importé
 */
public record CatalogSnapshot(
        @Nullable Subject subject,
        Map<UUID, ExistingTheme> themes,
        Map<UUID, ExistingNotion> notions,
        Map<UUID, ExistingCard> cards) {

    public CatalogSnapshot {
        themes = Map.copyOf(themes);
        notions = Map.copyOf(notions);
        cards = Map.copyOf(cards);
    }

    public static CatalogSnapshot empty() {
        return new CatalogSnapshot(null, Map.of(), Map.of(), Map.of());
    }

    public record ExistingTheme(UUID id, String slug, String title, int position, boolean retired) {}

    /** @param notion la notion telle qu'elle a été importée la dernière fois */
    public record ExistingNotion(UUID id, ParsedNotion notion, boolean retired) {}

    /** @param notionId la notion de la carte, {@code null} pour une carte d'un document Markdown */
    public record ExistingCard(
            UUID id,
            UUID themeId,
            @Nullable UUID notionId,
            CardType type,
            int position,
            int currentVersion,
            String contentHash,
            boolean retired) {}
}
