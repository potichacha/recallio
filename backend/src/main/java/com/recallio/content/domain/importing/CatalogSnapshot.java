package com.recallio.content.domain.importing;

import com.recallio.content.CardType;
import java.util.Map;
import java.util.UUID;

/** État du catalogue en base avant un import : ce à quoi on compare la source. */
public record CatalogSnapshot(Map<UUID, ExistingTheme> themes, Map<UUID, ExistingCard> cards) {

    public CatalogSnapshot {
        themes = Map.copyOf(themes);
        cards = Map.copyOf(cards);
    }

    public static CatalogSnapshot empty() {
        return new CatalogSnapshot(Map.of(), Map.of());
    }

    public record ExistingTheme(UUID id, String slug, String title, int position) {}

    public record ExistingCard(
            UUID id,
            UUID themeId,
            CardType type,
            int position,
            int currentVersion,
            String contentHash,
            boolean retired) {}
}
