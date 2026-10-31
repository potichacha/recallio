package com.recallio.content.domain.catalog;

import com.recallio.content.CardType;
import com.recallio.content.domain.Subject;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture du catalogue : sujets, thèmes et cartes actives (les cartes retirées n'apparaissent jamais). Les sujets
 * en brouillon n'apparaissent que si la visibilité demandée les inclut.
 */
public interface ContentCatalog {

    /** Ordonnés par position. */
    List<SubjectSummary> subjects(Visibility visibility);

    Optional<SubjectSummary> subjectBySlug(String slug, Visibility visibility);

    /** Les thèmes d'un sujet, dans l'ordre du document. */
    List<ThemeSummary> themes(UUID subjectId);

    Optional<ThemeSummary> themeBySlug(String slug, Visibility visibility);

    Page<CardView> activeCards(UUID themeId, int page, int size);

    /** Les brouillons ne sont visibles que des admins, qui relisent un sujet avant de le publier (CLAUDE.md, 2.5). */
    enum Visibility {
        PUBLISHED_ONLY,
        INCLUDING_DRAFTS
    }

    record SubjectSummary(
            UUID id,
            String slug,
            String title,
            Subject.Category category,
            int position,
            Subject.Status status,
            long themeCount,
            long cardCount) {}

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
