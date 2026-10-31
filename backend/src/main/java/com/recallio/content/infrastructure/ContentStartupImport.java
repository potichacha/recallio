package com.recallio.content.infrastructure;

import com.recallio.content.application.ContentImportService;
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
 * Importe au démarrage tous les documents de référence d'un dossier ({@code recallio.content.source-directory}), un
 * sujet par fichier {@code *-entretien-reference.md}, si {@code recallio.content.import-on-startup=true} (activé en
 * dev et dans le compose local). En production, l'import passe par l'endpoint d'administration.
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
        LOG.infof(
                "Import du contenu au démarrage : %d document(s) dans %s",
                sources.size(), directory.toAbsolutePath().normalize());
        // Un import (une transaction) par sujet : un document invalide n'empêche pas d'importer les autres.
        for (Path source : sources) {
            try {
                importService.importContent(read(source));
            } catch (RuntimeException e) {
                LOG.errorf(e, "Import impossible de %s", source.getFileName());
            }
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
    }
}
