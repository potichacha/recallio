package com.recallio.notification.infrastructure;

import com.recallio.notification.domain.PushPorts.PushSubscriptionRepository;
import com.recallio.notification.domain.PushSubscription;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class JpaPushSubscriptionRepository implements PushSubscriptionRepository {

    private final EntityManager entityManager;
    private final Clock clock;

    public JpaPushSubscriptionRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /** Une adresse appartient à un appareil : s'il change d'utilisateur, l'abonnement suit le dernier. */
    @Override
    public void save(PushSubscription subscription) {
        entityManager
                .createNativeQuery("""
                        insert into push_subscription (id, user_id, endpoint, p256dh, auth, reminder_hour, created_at)
                        values (:id, :userId, :endpoint, :p256dh, :auth, :hour, :now)
                        on conflict (endpoint) do update
                        set user_id = excluded.user_id, p256dh = excluded.p256dh, auth = excluded.auth,
                            reminder_hour = excluded.reminder_hour
                        """)
                .setParameter("id", subscription.id())
                .setParameter("userId", subscription.userId())
                .setParameter("endpoint", subscription.endpoint())
                .setParameter("p256dh", subscription.p256dh())
                .setParameter("auth", subscription.auth())
                .setParameter("hour", subscription.reminderHour())
                .setParameter("now", clock.instant())
                .executeUpdate();
    }

    @Override
    public void delete(UUID userId, String endpoint) {
        entityManager
                .createNativeQuery("delete from push_subscription where user_id = :userId and endpoint = :endpoint")
                .setParameter("userId", userId)
                .setParameter("endpoint", endpoint)
                .executeUpdate();
    }

    @Override
    public void delete(UUID subscriptionId) {
        entityManager
                .createNativeQuery("delete from push_subscription where id = :id")
                .setParameter("id", subscriptionId)
                .executeUpdate();
    }

    @Override
    public List<PushSubscription> all() {
        List<?> rows = entityManager.createNativeQuery("""
                        select id, user_id, endpoint, p256dh, auth, reminder_hour, last_reminded_on
                        from push_subscription order by created_at
                        """).getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;
                    return new PushSubscription(
                            (UUID) columns[0],
                            (UUID) columns[1],
                            (String) columns[2],
                            (String) columns[3],
                            (String) columns[4],
                            ((Number) columns[5]).intValue(),
                            day(columns[6]));
                })
                .toList();
    }

    @Override
    public boolean claim(UUID subscriptionId, LocalDate day) {
        return entityManager
                        .createNativeQuery("""
                                update push_subscription set last_reminded_on = :day
                                where id = :id and last_reminded_on is distinct from :day
                                """)
                        .setParameter("day", day)
                        .setParameter("id", subscriptionId)
                        .executeUpdate()
                > 0;
    }

    private static @Nullable LocalDate day(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate date -> date;
            case Date date -> date.toLocalDate();
            default -> throw new IllegalStateException("date inattendue : " + value.getClass());
        };
    }
}
