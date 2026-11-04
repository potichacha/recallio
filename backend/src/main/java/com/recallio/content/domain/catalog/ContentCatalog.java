package com.recallio.content.domain.catalog;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.TourPage;
import com.recallio.content.domain.Subject;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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

    /**
     * Le sommaire du Tour d'un sujet (ADR 0037) : ses notions qui ont des pages, dans l'ordre du référentiel, sans le
     * contenu des pages.
     */
    List<TourEntry> tour(UUID subjectId);

    /** Les pages d'une notion du Tour ; vide si la notion est inconnue, retirée ou hors du Tour. */
    Optional<TourNotionPages> tourNotion(UUID subjectId, String notionSlug);

    /**
     * Une notion du sommaire du Tour.
     *
     * @param domainTitle le titre du domaine, celui de son thème
     * @param title le titre de sa première page, ou celui de la notion
     */
    record TourEntry(
            String slug,
            String domain,
            String domainTitle,
            String chapter,
            String chapterTitle,
            String title,
            int pageCount) {}

    /** Les pages d'une notion du Tour, dans l'ordre de son fichier. */
    record TourNotionPages(String slug, String title, String chapterTitle, List<TourPage> pages) {

        public TourNotionPages {
            pages = List.copyOf(pages);
        }
    }

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
    /**
     * @param choices les choix d'un QCM, bonnes réponses comprises (le catalogue montre aussi les réponses)
     * @param blanks les trous d'un code à trous et leurs réponses acceptées
     */
    record CardView(
            UUID id,
            CardType type,
            String prompt,
            String answer,
            List<CardChoice> choices,
            List<CardBlank> blanks,
            @Nullable ExerciseFiles exercise,
            int version) {

        public CardView {
            choices = List.copyOf(choices);
            blanks = List.copyOf(blanks);
        }
    }
}
