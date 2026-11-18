package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.funnel.FunnelEventRepository;
import com.recallio.gamification.domain.funnel.FunnelStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JpaFunnelEventRepository implements FunnelEventRepository {

    private final EntityManager entityManager;

    public JpaFunnelEventRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void record(UUID id, FunnelStep step, Instant occurredAt) {
        entityManager
                .createNativeQuery("insert into funnel_event (id, step, occurred_at) values (:id, :step, :at)")
                .setParameter("id", id)
                .setParameter("step", step.name())
                .setParameter("at", occurredAt)
                .executeUpdate();
    }

    @Override
    public Map<FunnelStep, Long> countsBetween(Instant from, Instant to) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select step, count(*) from funnel_event
                        where occurred_at >= :from and occurred_at < :to
                        group by step
                        """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<FunnelStep, Long> counts = new EnumMap<>(FunnelStep.class);
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            counts.put(FunnelStep.valueOf((String) columns[0]), ((Number) columns[1]).longValue());
        }
        return counts;
    }
}
