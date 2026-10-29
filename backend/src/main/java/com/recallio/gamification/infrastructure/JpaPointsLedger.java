package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.PointsLedger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.UUID;

@ApplicationScoped
public class JpaPointsLedger implements PointsLedger {

    private final EntityManager entityManager;

    public JpaPointsLedger(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO NOTHING} : filet de sécurité si une même révision était traitée deux fois. */
    @Override
    public void append(PointsEntry entry) {
        entityManager
                .createNativeQuery("""
                        insert into points_ledger (id, user_id, amount, reason, source_id, occurred_at)
                        values (:id, :userId, :amount, :reason, :sourceId, :occurredAt)
                        on conflict (user_id, reason, source_id) do nothing
                        """)
                .setParameter("id", entry.id())
                .setParameter("userId", entry.userId())
                .setParameter("amount", entry.amount())
                .setParameter("reason", entry.reason().name())
                .setParameter("sourceId", entry.sourceId())
                .setParameter("occurredAt", entry.occurredAt())
                .executeUpdate();
    }

    @Override
    public long total(UUID userId) {
        Number total = (Number) entityManager
                .createNativeQuery("select coalesce(sum(amount), 0) from points_ledger where user_id = :userId")
                .setParameter("userId", userId)
                .getSingleResult();
        return total.longValue();
    }
}
