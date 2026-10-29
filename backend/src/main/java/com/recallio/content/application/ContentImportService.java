package com.recallio.content.application;

import com.recallio.content.domain.importing.CatalogStore;
import com.recallio.content.domain.importing.ImportPlan;
import com.recallio.content.domain.importing.ImportPlanner;
import com.recallio.content.domain.importing.ImportReport;
import com.recallio.content.domain.source.ContentParser;
import com.recallio.content.domain.source.ParsedContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/** Cas d'usage : importer le contenu source dans le catalogue, de façon idempotente, et produire un rapport. */
@ApplicationScoped
public class ContentImportService {

    private static final Logger LOG = Logger.getLogger(ContentImportService.class);

    private final ContentParser parser;
    private final CatalogStore store;
    private final ImportPlanner planner = new ImportPlanner();

    public ContentImportService(ContentParser parser, CatalogStore store) {
        this.parser = parser;
        this.store = store;
    }

    /** L'import est atomique : en cas d'erreur, le catalogue reste dans son état précédent. */
    @Transactional
    public ImportReport importContent(String source) {
        ParsedContent parsed = parser.parse(source);
        ImportPlan plan = planner.plan(parsed, store.snapshot());
        store.apply(plan);
        ImportReport report = plan.report();
        LOG.infof(
                "Import du contenu : %d ajoutées, %d modifiées, %d inchangées, %d retirées, %d réactivées, %d anomalies",
                report.cardsAdded(),
                report.cardsUpdated(),
                report.cardsUnchanged(),
                report.cardsRetired(),
                report.cardsReactivated(),
                report.issues().size() + report.duplicates().size());
        report.issues()
                .forEach(issue -> LOG.warnf("Ligne %d : %s (%s)", issue.line(), issue.reason(), issue.excerpt()));
        report.duplicates()
                .forEach(duplicate -> LOG.warnf(
                        "Ligne %d : question en double dans le thème %s, ignorée (%s)",
                        duplicate.line(), duplicate.themeKey(), duplicate.prompt()));
        return report;
    }
}
