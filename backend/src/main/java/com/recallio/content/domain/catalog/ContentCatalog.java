package com.recallio.content.domain.catalog;

import com.recallio.content.CardType;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture du catalogue publié : thèmes et cartes actives des sujets publiés (les cartes retirées et les sujets en
 * brouillon n'apparaissent jamais).
 */
public interface ContentCatalog {

    List<ThemeSummary> themes();

    Optional<ThemeSummary> themeBySlug(String slug);

    Page<CardView> activeCards(UUID themeId, int page, int size);

    /** Ordonnés par sujet, puis dans l'ordre du document. */
    record ThemeSummary(
            UUID id,
            String subjectSlug,
            String subjectTitle,
            String slug,
            String title,
            int position,
            long cardCount) {}

    /** La version courante d'une carte. */
    record CardView(UUID id, CardType type, String prompt, String answer, int version) {}
}
