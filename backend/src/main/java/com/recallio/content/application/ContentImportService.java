package com.recallio.content.application;

import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.importing.CatalogStore;
import com.recallio.content.domain.importing.ImportPlan;
import com.recallio.content.domain.importing.ImportPlanner;
import com.recallio.content.domain.importing.ImportReport;
import com.recallio.content.domain.source.ContentParser;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.content.domain.source.SubjectFolderParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Cas d'usage : importer la source d'un sujet dans le catalogue, de façon idempotente, et produire un rapport. La source
 * est un document Markdown, ou le dossier du sujet (référentiel de notions et fichiers de notion, ADR 0026).
 */
@ApplicationScoped
public class ContentImportService {

    private static final Logger LOG = Logger.getLogger(ContentImportService.class);

    private final ContentParser parser;
    private final SubjectFolderParser folderParser;
    private final CatalogStore store;
    private final ImportPlanner planner = new ImportPlanner();

    public ContentImportService(ContentParser parser, SubjectFolderParser folderParser, CatalogStore store) {
        this.parser = parser;
        this.folderParser = folderParser;
        this.store = store;
    }

    /**
     * Importe un document Markdown. L'import est atomique : en cas d'erreur, le catalogue reste dans son état
     * précédent.
     */
    @Transactional
    public ImportReport importContent(String source) {
        return importParsed(parser.parse(source));
    }

    /** Importe le dossier d'un sujet, atomiquement lui aussi. */
    @Transactional
    public ImportReport importFolder(SubjectFolder folder) {
        return importParsed(folderParser.parse(folder));
    }

    private ImportReport importParsed(ParsedContent parsed) {
        ImportPlan plan = planner.plan(
                parsed, store.snapshot(CardIdentity.subjectId(parsed.subject().slug())));
        store.apply(plan);
        ImportReport report = plan.report();
        LOG.infof(
                "Import du sujet %s : %d ajoutées, %d modifiées, %d inchangées, %d retirées, %d réactivées, %d notions"
                        + " créées ou modifiées, %d thèmes retirés, %d anomalies",
                report.subject(),
                report.cardsAdded(),
                report.cardsUpdated(),
                report.cardsUnchanged(),
                report.cardsRetired(),
                report.cardsReactivated(),
                plan.notions().size(),
                plan.retiredThemeIds().size(),
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
