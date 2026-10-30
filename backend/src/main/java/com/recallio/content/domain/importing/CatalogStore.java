package com.recallio.content.domain.importing;

import java.util.UUID;

/** Port de persistance utilisé par l'import : lire l'état d'un sujet, puis appliquer un plan. */
public interface CatalogStore {

    CatalogSnapshot snapshot(UUID subjectId);

    void apply(ImportPlan plan);
}
