package com.recallio.content.infrastructure;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.infrastructure.folder.SubjectFolders;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 * Importe au démarrage, si {@code recallio.content.import-on-startup=true} (activé en dev et dans le compose local) :
 *
 * <ul>
 *   <li>tous les documents de référence d'un dossier ({@code recallio.content.source-directory}), un sujet par fichier
 *       {@code *-entretien-reference.md} ;
 *   <li>tous les dossiers de sujet ({@code recallio.content.subjects-directory}, ADR 0026), un sujet par dossier qui
 *       a un {@code subject.yaml}.
 * </ul>
 *
 * <p>En production, l'import d'un document passe par l'endpoint d'administration ; celui des dossiers de sujet se
 * décidera avec le déploiement.
 */
@ApplicationScoped
public class ContentStartupImport {

    private static final Logger LOG = Logger.getLogger(ContentStartupImport.class);
    static final String SOURCE_SUFFIX = "-entretien-reference.md";

    private final ContentImportService importService;
    private final ContentConfig config;

    public ContentStartupImport(ContentImportService importService, ContentConfig config) {
        this.importService = importService;
        this.config = config;
    }

    void onStart(@Observes StartupEvent event) {
        if (!config.importOnStartup()) {
            return;
        }
        Path directory = config.sourceDirectory()
                .map(Path::of)
                .orElseThrow(() -> new IllegalStateException(
                        "recallio.content.source-directory est obligatoire quand import-on-startup est activé"));
        List<Path> sources = sourcesIn(directory);
        List<Path> folders =
                config.subjectsDirectory().map(Path::of).map(SubjectFolders::in).orElse(List.of());
        LOG.infof(
                "Import du contenu au démarrage : %d document(s) dans %s, %d dossier(s) de sujet",
                sources.size(), directory.toAbsolutePath().normalize(), folders.size());
        // Un import (une transaction) par sujet : une source invalide n'empêche pas d'importer les autres.
        sources.forEach(source -> importSafely(source, () -> importService.importContent(read(source))));
        folders.forEach(folder -> importSafely(folder, () -> importService.importFolder(SubjectFolders.read(folder))));
    }

    private static void importSafely(Path source, Runnable importation) {
        try {
            importation.run();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Import impossible de %s", source.getFileName());
        }
    }

    /** Les documents du dossier, dans l'ordre alphabétique (journal d'import reproductible). */
    static List<Path> sourcesIn(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(SOURCE_SUFFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister le dossier de contenu " + directory, e);
        }
    }

    private static String read(Path source) {
        try {
            return Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le contenu source " + source, e);
        }
    }

    @ConfigMapping(prefix = "recallio.content")
    public interface ContentConfig {

        @WithDefault("false")
        boolean importOnStartup();

        /** Dossier des documents de référence ({@code content/source}). */
        Optional<String> sourceDirectory();

        /** Dossier qui contient les dossiers de sujet ({@code content}). */
        Optional<String> subjectsDirectory();
    }
}
