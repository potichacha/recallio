package com.recallio.live.application;

import com.recallio.live.domain.LiveTiming;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.Duration;

/** Le format d'une partie live ({@code recallio.live.*}, ADR 0022). */
@ConfigMapping(prefix = "recallio.live")
public interface LiveConfig {

    /** Questions tirées pour une partie, si le sujet en compte assez. */
    @WithDefault("10")
    int questionsPerGame();

    /** En dessous, une partie n'a pas d'intérêt : le démarrage est refusé. */
    @WithDefault("3")
    int minQuestions();

    /** Le chrono d'une question : court, c'est le principe du mode live (CLAUDE.md, section 6.5). */
    @WithDefault("20")
    int questionSeconds();

    /** Le temps de lire la correction avant la question suivante. */
    @WithDefault("5")
    int revealSeconds();

    default LiveTiming timing() {
        return new LiveTiming(Duration.ofSeconds(questionSeconds()), Duration.ofSeconds(revealSeconds()));
    }
}
