package com.recallio.content.application;

import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.content.domain.catalog.ContentCatalog.CardView;
import com.recallio.content.domain.catalog.ContentCatalog.ThemeSummary;
import com.recallio.content.domain.catalog.ThemeNotFoundException;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/** Cas d'usage : consulter le catalogue publié (thèmes et cartes actives). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ContentBrowsingService {

    private final ContentCatalog catalog;

    public ContentBrowsingService(ContentCatalog catalog) {
        this.catalog = catalog;
    }

    public List<ThemeSummary> themes() {
        return catalog.themes();
    }

    public ThemeSummary theme(String slug) {
        return catalog.themeBySlug(slug).orElseThrow(() -> new ThemeNotFoundException(slug));
    }

    public Page<CardView> cards(String themeSlug, int page, int size) {
        return catalog.activeCards(theme(themeSlug).id(), page, size);
    }
}
