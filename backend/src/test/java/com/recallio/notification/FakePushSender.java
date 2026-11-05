package com.recallio.notification;

import com.recallio.notification.domain.PushPorts.Delivery;
import com.recallio.notification.domain.PushPorts.PushSender;
import com.recallio.notification.domain.PushSubscription;
import com.recallio.notification.domain.Reminder;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Le service de notification dans les tests du backend (ADR 0047) : il garde les rappels au lieu de les envoyer, et
 * chaque test choisit la réponse du service.
 */
@Mock
@ApplicationScoped
public class FakePushSender implements PushSender {

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private volatile Delivery answer = Delivery.SENT;

    @Override
    public Delivery send(PushSubscription subscription, Reminder reminder) {
        sent.add(new Sent(subscription, reminder));
        return answer;
    }

    @Override
    public String publicKey() {
        return "BFakeVapidPublicKey";
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }

    public void reset(Delivery next) {
        sent.clear();
        answer = next;
    }

    public record Sent(PushSubscription subscription, Reminder reminder) {}
}
