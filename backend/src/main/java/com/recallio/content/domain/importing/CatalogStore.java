package com.recallio.content.domain.importing;

/** Port de persistance utilisé par l'import : lire l'état du catalogue, puis appliquer un plan. */
public interface CatalogStore {

    CatalogSnapshot snapshot();

    void apply(ImportPlan plan);
}
