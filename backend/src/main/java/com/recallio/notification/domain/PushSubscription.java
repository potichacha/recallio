package com.recallio.notification.domain;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Un abonnement Web Push (ADR 0047) : un appareil où l'utilisateur a accepté les notifications.
 *
 * @param endpoint l'adresse du service de notification du navigateur
 * @param p256dh la clé publique de l'appareil, en base64url : elle chiffre les messages
 * @param auth le secret d'authentification de l'appareil, en base64url
 * @param reminderHour l'heure du rappel, de 0 à 23, dans le fuseau de l'utilisateur
 * @param lastRemindedOn le dernier jour où il a reçu un rappel ; {@code null} s'il n'en a jamais reçu
 */
public record PushSubscription(
        UUID id,
        UUID userId,
        String endpoint,
        String p256dh,
        String auth,
        int reminderHour,
        @Nullable LocalDate lastRemindedOn) {

    /**
     * Les services de notification des navigateurs : le serveur n'envoie qu'à eux, jamais à une adresse quelconque
     * qu'un client lui donnerait (SSRF).
     */
    static final List<String> PUSH_SERVICES = List.of(
            "fcm.googleapis.com",
            "android.googleapis.com",
            "updates.push.services.mozilla.com",
            "push.services.mozilla.com",
            "push.apple.com",
            "notify.windows.com");

    public PushSubscription {
        if (reminderHour < 0 || reminderHour > 23) {
            throw new IllegalArgumentException("Une heure va de 0 à 23 : " + reminderHour);
        }
        if (!isPushService(endpoint)) {
            throw new InvalidPushSubscriptionException();
        }
    }

    /** Vrai pour une adresse https d'un service de notification connu. */
    public static boolean isPushService(String endpoint) {
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
        String host = uri.getHost();
        if (!"https".equals(uri.getScheme()) || host == null) {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return PUSH_SERVICES.stream().anyMatch(service -> lower.equals(service) || lower.endsWith("." + service));
    }
}
