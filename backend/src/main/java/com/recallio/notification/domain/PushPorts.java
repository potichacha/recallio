package com.recallio.notification.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Les ports des rappels (ADR 0047) : les abonnements, et l'envoi d'une notification. */
public final class PushPorts {

    private PushPorts() {}

    /** Les abonnements, dans la table {@code push_subscription}. */
    public interface PushSubscriptionRepository {

        /** Crée l'abonnement, ou le remplace s'il existe déjà pour cette adresse. */
        void save(PushSubscription subscription);

        void delete(UUID userId, String endpoint);

        void delete(UUID subscriptionId);

        List<PushSubscription> all();

        /**
         * Réserve le rappel du jour de cet abonnement.
         *
         * @return faux s'il était déjà réservé pour ce jour : deux instances n'envoient pas deux rappels
         */
        boolean claim(UUID subscriptionId, LocalDate day);
    }

    /** L'envoi d'une notification Web Push, chiffrée pour l'appareil. */
    public interface PushSender {

        Delivery send(PushSubscription subscription, Reminder reminder);

        /** La clé publique VAPID du serveur, en base64url : le navigateur la demande pour s'abonner. */
        String publicKey();
    }

    public enum Delivery {
        SENT,
        /** L'appareil s'est désabonné : l'abonnement est à oublier. */
        GONE,
        FAILED
    }
}
