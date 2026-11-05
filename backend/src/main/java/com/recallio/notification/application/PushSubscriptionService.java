package com.recallio.notification.application;

import com.recallio.notification.domain.PushPorts.PushSender;
import com.recallio.notification.domain.PushPorts.PushSubscriptionRepository;
import com.recallio.notification.domain.PushSubscription;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Cas d'usage : accepter les rappels sur un appareil, ou les retirer (ADR 0047). */
@ApplicationScoped
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptions;
    private final PushSender sender;
    private final IdGenerator ids;

    public PushSubscriptionService(PushSubscriptionRepository subscriptions, PushSender sender, IdGenerator ids) {
        this.subscriptions = subscriptions;
        this.sender = sender;
        this.ids = ids;
    }

    /** La clé publique du serveur, que le navigateur demande pour s'abonner. */
    public String publicKey() {
        return sender.publicKey();
    }

    /**
     * @throws com.recallio.notification.domain.InvalidPushSubscriptionException si l'adresse n'est pas celle d'un
     *     service de notification connu
     */
    @Transactional
    public void subscribe(UserRef user, String endpoint, String p256dh, String auth, int reminderHour) {
        subscriptions.save(new PushSubscription(ids.newId(), user.id(), endpoint, p256dh, auth, reminderHour, null));
    }

    @Transactional
    public void unsubscribe(UserRef user, String endpoint) {
        subscriptions.delete(user.id(), endpoint);
    }
}
