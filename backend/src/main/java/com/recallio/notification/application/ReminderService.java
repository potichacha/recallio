package com.recallio.notification.application;

import com.recallio.notification.application.ReminderPlanner.Due;
import com.recallio.notification.domain.PushPorts.Delivery;
import com.recallio.notification.domain.PushPorts.PushSender;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import org.jboss.logging.Logger;

/**
 * Cas d'usage : envoyer les rappels du jour (ADR 0047). Au plus un par jour et par appareil, à partir de l'heure
 * choisie, et seulement si l'objectif du jour n'est pas atteint. L'envoi se fait hors transaction : un service de
 * notification lent ne bloque pas la base.
 */
@ApplicationScoped
public class ReminderService {

    private static final Logger LOG = Logger.getLogger(ReminderService.class);

    private final ReminderPlanner planner;
    private final PushSender sender;
    private final Clock clock;

    public ReminderService(ReminderPlanner planner, PushSender sender, Clock clock) {
        this.planner = planner;
        this.sender = sender;
        this.clock = clock;
    }

    /** @return les rappels envoyés */
    public int sendDue() {
        int sent = 0;
        for (Due due : planner.claimDue(clock.instant())) {
            Delivery delivery = sender.send(due.subscription(), due.reminder());
            if (delivery == Delivery.GONE) {
                planner.forget(due.subscription());
            }
            if (delivery == Delivery.SENT) {
                sent++;
            }
        }
        if (sent > 0) {
            LOG.infof("%d rappel(s) envoyé(s)", sent);
        }
        return sent;
    }
}
