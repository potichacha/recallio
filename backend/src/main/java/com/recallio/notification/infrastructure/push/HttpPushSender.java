package com.recallio.notification.infrastructure.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallio.notification.application.PushConfig;
import com.recallio.notification.domain.PushPorts.Delivery;
import com.recallio.notification.domain.PushPorts.PushSender;
import com.recallio.notification.domain.PushSubscription;
import com.recallio.notification.domain.Reminder;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * L'envoi d'un rappel par le protocole Web Push : le message chiffré pour l'appareil (RFC 8291), le serveur identifié
 * par VAPID (RFC 8292). Sans clés configurées, le serveur en tire une paire au démarrage : les abonnements ne survivent
 * alors pas à un redémarrage, ce qui ne convient qu'au développement.
 */
@ApplicationScoped
public class HttpPushSender implements PushSender {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** Combien de temps le service garde un rappel pour un appareil éteint : au-delà, il n'a plus de sens. */
    static final int TIME_TO_LIVE_SECONDS = 12 * 60 * 60;

    private static final int GONE = 410;
    private static final int NOT_FOUND = 404;
    private static final Logger LOG = Logger.getLogger(HttpPushSender.class);

    private final KeyPair vapidKeys;
    private final String subject;
    private final ObjectMapper json;
    private final Clock clock;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final SecureRandom random = new SecureRandom();

    public HttpPushSender(PushConfig config, ObjectMapper json, Clock clock) {
        this.subject = config.subject();
        this.json = json;
        this.clock = clock;
        if (config.vapidPublicKey().isPresent() && config.vapidPrivateKey().isPresent()) {
            this.vapidKeys = EcKeys.pair(
                    config.vapidPublicKey().get(), config.vapidPrivateKey().get());
        } else {
            LOG.warn("Clés VAPID absentes : une paire éphémère est tirée. En production, définir"
                    + " RECALLIO_PUSH_VAPID_PUBLIC_KEY et RECALLIO_PUSH_VAPID_PRIVATE_KEY.");
            this.vapidKeys = EcKeys.generate();
        }
    }

    @Override
    public String publicKey() {
        return EcKeys.base64Url(EcKeys.encode((ECPublicKey) vapidKeys.getPublic()));
    }

    @Override
    public Delivery send(PushSubscription subscription, Reminder reminder) {
        byte[] salt = new byte[WebPushEncryption.SALT_LENGTH];
        random.nextBytes(salt);
        byte[] body;
        try {
            body = WebPushEncryption.encrypt(
                    json.writeValueAsBytes(
                            Map.of("title", reminder.title(), "body", reminder.body(), "url", reminder.url())),
                    EcKeys.publicKey(EcKeys.decode(subscription.p256dh())),
                    EcKeys.decode(subscription.auth()),
                    EcKeys.generate(),
                    salt);
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            LOG.warnf("Abonnement %s illisible : %s", subscription.id(), invalid.getMessage());
            return Delivery.GONE;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(subscription.endpoint()))
                .timeout(TIMEOUT)
                .header("TTL", String.valueOf(TIME_TO_LIVE_SECONDS))
                .header("Content-Encoding", "aes128gcm")
                .header("Content-Type", "application/octet-stream")
                .header(
                        "Authorization",
                        Vapid.authorization(subscription.endpoint(), vapidKeys, subject, clock.instant()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            int status =
                    http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == GONE || status == NOT_FOUND) {
                return Delivery.GONE;
            }
            if (status / 100 != 2) {
                LOG.warnf("Rappel %s refusé par le service de notification : %d", subscription.id(), status);
                return Delivery.FAILED;
            }
            return Delivery.SENT;
        } catch (IOException failure) {
            LOG.warnf("Rappel %s non envoyé : %s", subscription.id(), failure.getMessage());
            return Delivery.FAILED;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Delivery.FAILED;
        }
    }
}
