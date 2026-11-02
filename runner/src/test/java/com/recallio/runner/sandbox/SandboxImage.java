package com.recallio.runner.sandbox;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * L'image du bac à sable, construite depuis {@code sandbox/java} avant les tests qui exécutent du code : le cache de
 * Docker rend la construction instantanée quand rien n'a changé.
 */
public final class SandboxImage {

    public static final String NAME = "recallio-sandbox-java";
    private static final String CONTEXT = "../sandbox/java";
    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);

    private static boolean built;

    private SandboxImage() {}

    public static synchronized void ensureBuilt() {
        if (built) {
            return;
        }
        try {
            Process build = new ProcessBuilder("docker", "build", "--quiet", "--tag", NAME, CONTEXT)
                    .inheritIO()
                    .start();
            if (!build.waitFor(BUILD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS) || build.exitValue() != 0) {
                throw new IllegalStateException("La construction de l'image " + NAME + " a échoué");
            }
            built = true;
        } catch (IOException e) {
            throw new IllegalStateException("Docker est nécessaire pour exécuter le bac à sable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Un bac à sable sur cette image, avec ce délai. */
    public static DockerSandbox sandbox(Duration timeout) {
        ensureBuilt();
        return new DockerSandbox(NAME, timeout, Optional.empty());
    }
}
