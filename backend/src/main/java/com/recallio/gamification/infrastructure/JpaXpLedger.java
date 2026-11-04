package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.XpLedger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Le registre de l'XP, dans la table {@code points_ledger} : elle a gardé son nom de la phase 1 (ADR 0035). */
@ApplicationScoped
public class JpaXpLedger implements XpLedger {

    private final EntityManager entityManager;

    public JpaXpLedger(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO NOTHING} : filet de sécurité si une même révision était traitée deux fois. */
    @Override
    public void append(XpEntry entry) {
        entityManager
                .createNativeQuery("""
                        insert into points_ledger (id, user_id, amount, reason, subject_slug, source_id, occurred_at)
                        values (:id, :userId, :amount, :reason, :subjectSlug, :sourceId, :occurredAt)
                        on conflict (user_id, reason, source_id) do nothing
                        """)
                .setParameter("id", entry.id())
                .setParameter("userId", entry.userId())
                .setParameter("amount", entry.amount())
                .setParameter("reason", entry.reason().name())
                .setParameter("subjectSlug", entry.subjectSlug())
                .setParameter("sourceId", entry.sourceId())
                .setParameter("occurredAt", entry.occurredAt())
                .executeUpdate();
    }

    /** Une seule lecture : la somme de chaque sujet, et celle des entrées sans sujet (les bonus du jour). */
    @Override
    public XpTotals totals(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select subject_slug, sum(amount)
                        from points_ledger
                        where user_id = :userId
                        group by subject_slug
                        """)
                .setParameter("userId", userId)
                .getResultList();
        long total = 0;
        Map<String, Long> bySubject = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            long amount = ((Number) columns[1]).longValue();
            total += amount;
            if (columns[0] instanceof String subjectSlug) {
                bySubject.put(subjectSlug, amount);
            }
        }
        return new XpTotals(total, bySubject);
    }
}
