package com.recallio.runner.sandbox;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Les images du bac à sable, une par langage, construites depuis {@code sandbox/<langage>} avant les tests qui exécutent
 * du code : le cache de Docker rend la construction instantanée quand rien n'a changé.
 */
public final class SandboxImage {

    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);

    private static final Set<Language> BUILT = EnumSet.noneOf(Language.class);

    private SandboxImage() {}

    /** Le nom de l'image d'un langage : {@code recallio-sandbox-java}, {@code recallio-sandbox-go}... */
    public static String name(Language language) {
        return "recallio-sandbox-" + language.name().toLowerCase(Locale.ROOT);
    }

    /** Le langage d'un fichier du contenu, d'après son extension (ADR 0033, 0053). */
    public static Language languageOf(String path) {
        if (path.endsWith(".go")) {
            return Language.GO;
        }
        if (path.endsWith(".cpp") || path.endsWith(".hpp")) {
            return Language.CPP;
        }
        return Language.JAVA;
    }

    /** Construit l'image de chaque langage. */
    public static synchronized void ensureBuilt() {
        for (Language language : Language.values()) {
            ensureBuilt(language);
        }
    }

    private static void ensureBuilt(Language language) {
        if (BUILT.contains(language)) {
            return;
        }
        String context = "../sandbox/" + language.name().toLowerCase(Locale.ROOT);
        try {
            Process build = new ProcessBuilder("docker", "build", "--quiet", "--tag", name(language), context)
                    .inheritIO()
                    .start();
            if (!build.waitFor(BUILD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS) || build.exitValue() != 0) {
                throw new IllegalStateException("La construction de l'image " + name(language) + " a échoué");
            }
            BUILT.add(language);
        } catch (IOException e) {
            throw new IllegalStateException("Docker est nécessaire pour exécuter le bac à sable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Le délai d'un programme du Tour, celui de la configuration (ADR 0037). */
    public static final Duration PROGRAM_TIMEOUT = Duration.ofSeconds(10);

    /** Un bac à sable sur les images de tous les langages, avec ce délai. */
    public static DockerSandbox sandbox(Duration timeout) {
        return sandbox(timeout, PROGRAM_TIMEOUT);
    }

    /** Un bac à sable sur les images de tous les langages, avec ces délais : celui du conteneur, celui d'un programme. */
    public static DockerSandbox sandbox(Duration timeout, Duration programTimeout) {
        ensureBuilt();
        Map<Language, String> images = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            images.put(language, name(language));
        }
        return new DockerSandbox(images, timeout, programTimeout, Optional.empty());
    }
}
