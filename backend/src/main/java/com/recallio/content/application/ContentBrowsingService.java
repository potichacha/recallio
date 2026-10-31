package com.recallio.content.application;

import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.content.domain.catalog.ContentCatalog.CardView;
import com.recallio.content.domain.catalog.ContentCatalog.SubjectSummary;
import com.recallio.content.domain.catalog.ContentCatalog.ThemeSummary;
import com.recallio.content.domain.catalog.ContentCatalog.Visibility;
import com.recallio.content.domain.catalog.SubjectNotFoundException;
import com.recallio.content.domain.catalog.ThemeNotFoundException;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Cas d'usage : consulter le catalogue (sujets, thèmes et cartes actives). Un sujet invisible pour l'appelant (brouillon
 * consulté par un non-admin) répond comme un sujet inexistant : son existence ne fuite pas.
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
}
