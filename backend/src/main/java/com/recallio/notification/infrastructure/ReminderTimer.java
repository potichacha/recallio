package com.recallio.notification.infrastructure;

import com.recallio.notification.application.PushConfig;
import com.recallio.notification.application.ReminderService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.time.Duration;
import org.jboss.logging.Logger;

/**
 * Le passage régulier des rappels (ADR 0047) : un minuteur Vert.x, comme le mode live, sans extension de planification.
 * L'envoi, bloquant, part sur un fil de travail. Plusieurs instances peuvent tourner : la réservation en base évite les
 * doublons.
 */
@ApplicationScoped
public class ReminderTimer {

    static final Duration PERIOD = Duration.ofMinutes(10);

    private static final Logger LOG = Logger.getLogger(ReminderTimer.class);

    private final Vertx vertx;
    private final ReminderService reminders;
    private final boolean enabled;
    private long timerId = -1;

    public ReminderTimer(Vertx vertx, ReminderService reminders, PushConfig config) {
        this.vertx = vertx;
        this.reminders = reminders;
        this.enabled = config.remindersEnabled();
    }

    void start(@Observes StartupEvent startup) {
        if (enabled) {
            timerId = vertx.setPeriodic(
                    PERIOD.toMillis(),
                    _ -> vertx.executeBlocking(() -> {
                        try {
                            return reminders.sendDue();
                        } catch (RuntimeException failure) {
                            LOG.error("Rappels non envoyés", failure);
                            return 0;
                        }
                    }));
        }
    }

    void stop(@Observes ShutdownEvent shutdown) {
        if (timerId >= 0) {
            vertx.cancelTimer(timerId);
        }
    }
}
