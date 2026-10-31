package com.recallio.content.api;

import com.recallio.content.domain.catalog.ContentCatalog.Visibility;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Ce que l'appelant du catalogue peut voir. Le catalogue est public, mais un admin connecté y voit aussi les sujets en
 * brouillon, pour les relire en conditions réelles avant de les publier.
 */
@ApplicationScoped
class CatalogVisibility {

    private final SecurityIdentity identity;

    CatalogVisibility(SecurityIdentity identity) {
        this.identity = identity;
    }

    Visibility current() {
        return identity.hasRole(ContentImportResource.ADMIN_ROLE)
                ? Visibility.INCLUDING_DRAFTS
                : Visibility.PUBLISHED_ONLY;
    }
}
