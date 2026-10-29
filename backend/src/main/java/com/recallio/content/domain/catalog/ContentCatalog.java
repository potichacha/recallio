package com.recallio.content.domain.catalog;

import com.recallio.content.CardType;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de lecture du catalogue publié : thèmes et cartes actives (les cartes retirées n'apparaissent jamais). */
public interface ContentCatalog {

    List<ThemeSummary> themes();

    Optional<ThemeSummary> themeBySlug(String slug);

    Page<CardView> activeCards(UUID themeId, int page, int size);

    record ThemeSummary(UUID id, String slug, String title, int position, long cardCount) {}

    /** La version courante d'une carte. */
    record CardView(UUID id, CardType type, String prompt, String answer, int version) {}
}
