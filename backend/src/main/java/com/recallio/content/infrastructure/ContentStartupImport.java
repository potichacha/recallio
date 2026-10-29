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
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Importe le contenu au démarrage si {@code recallio.content.import-on-startup=true} (activé en dev et dans le compose
 * local). En production, l'import passe par l'endpoint d'administration.
 */
@ApplicationScoped
public class ContentStartupImport {

    private static final Logger LOG = Logger.getLogger(ContentStartupImport.class);

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
        Path source = config.sourcePath()
                .map(Path::of)
                .orElseThrow(() -> new IllegalStateException(
                        "recallio.content.source-path est obligatoire quand import-on-startup est activé"));
        LOG.infof(
                "Import du contenu au démarrage depuis %s",
                source.toAbsolutePath().normalize());
        importService.importContent(read(source));
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

        Optional<String> sourcePath();
    }
}
