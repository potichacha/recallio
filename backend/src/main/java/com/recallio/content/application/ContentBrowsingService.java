package com.recallio.content.application;

import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.content.domain.catalog.ContentCatalog.CardView;
import com.recallio.content.domain.catalog.ContentCatalog.NotionLesson;
import com.recallio.content.domain.catalog.ContentCatalog.SubjectSummary;
import com.recallio.content.domain.catalog.ContentCatalog.ThemeSummary;
import com.recallio.content.domain.catalog.ContentCatalog.TourNotionPages;
import com.recallio.content.domain.catalog.ContentCatalog.Visibility;
import com.recallio.content.domain.catalog.NotionNotFoundException;
import com.recallio.content.domain.catalog.SubjectNotFoundException;
import com.recallio.content.domain.catalog.ThemeNotFoundException;
import com.recallio.content.domain.catalog.TourNotionNotFoundException;
import com.recallio.content.domain.catalog.TourOutline;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Cas d'usage : consulter le catalogue (sujets, thèmes et cartes actives) et le Tour de ses sujets (ADR 0037). Un sujet
 * invisible pour l'appelant (brouillon consulté par un non-admin) répond comme un sujet inexistant : son existence ne
 * fuite pas.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ContentBrowsingService {

    private final ContentCatalog catalog;

    public ContentBrowsingService(ContentCatalog catalog) {
        this.catalog = catalog;
    }

    public List<SubjectSummary> subjects(Visibility visibility) {
        return catalog.subjects(visibility);
    }

    public SubjectSummary subject(String slug, Visibility visibility) {
        return catalog.subjectBySlug(slug, visibility).orElseThrow(() -> new SubjectNotFoundException(slug));
    }

    public List<ThemeSummary> themes(String subjectSlug, Visibility visibility) {
        return catalog.themes(subject(subjectSlug, visibility).id());
    }

    public ThemeSummary theme(String slug, Visibility visibility) {
        return catalog.themeBySlug(slug, visibility).orElseThrow(() -> new ThemeNotFoundException(slug));
    }

    public Page<CardView> cards(String themeSlug, Visibility visibility, int page, int size) {
        return catalog.activeCards(theme(themeSlug, visibility).id(), page, size);
    }

    /** Le sommaire du Tour d'un sujet (ADR 0037) ; sans domaine si le sujet n'a pas encore de Tour. */
    public TourOutline tour(String subjectSlug, Visibility visibility) {
        SubjectSummary subject = subject(subjectSlug, visibility);
        return TourOutline.of(
                subject.slug(), subject.title(), catalog.tour(subject.id()), catalog.tourExercises(subject.id()));
    }

    /** @throws NotionNotFoundException si la notion n'existe pas dans ce sujet */
    public NotionLesson notionLesson(String subjectSlug, String notionSlug, Visibility visibility) {
        return catalog.notionLesson(subject(subjectSlug, visibility).id(), notionSlug)
                .orElseThrow(() -> new NotionNotFoundException(subjectSlug, notionSlug));
    }

    /** @throws TourNotionNotFoundException si la notion n'a pas de page dans le Tour de ce sujet */
    public TourNotionPages tourNotion(String subjectSlug, String notionSlug, Visibility visibility) {
        return catalog.tourNotion(subject(subjectSlug, visibility).id(), notionSlug)
                .orElseThrow(() -> new TourNotionNotFoundException(subjectSlug, notionSlug));
    }
}
