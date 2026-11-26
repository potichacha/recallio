package com.recallio.review.infrastructure;

import com.recallio.review.domain.drill.DrillActivityRepository;
import com.recallio.review.domain.drill.DrillUsage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JpaDrillActivityRepository implements DrillActivityRepository {

    private final EntityManager entityManager;

    public JpaDrillActivityRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO UPDATE} : deux réponses simultanées comptent toutes les deux, sans lecture préalable. */
    @Override
    public DrillUsage record(UUID userId, LocalDate day, boolean correct) {
        Object[] row = (Object[]) entityManager
                .createNativeQuery("""
                        insert into drill_activity (user_id, local_date, answers, correct)
                        values (:userId, :day, 1, :correct)
                        on conflict (user_id, local_date) do update
                        set answers = drill_activity.answers + 1, correct = drill_activity.correct + excluded.correct
                        returning answers, correct
                        """)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .setParameter("correct", correct ? 1 : 0)
                .getSingleResult();
        return usage(day, row);
    }

    @Override
    public DrillUsage find(UUID userId, LocalDate day) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select answers, correct from drill_activity
                        where user_id = :userId and local_date = :day
                        """)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .getResultList();
        return rows.isEmpty() ? DrillUsage.none(day) : usage(day, (Object[]) rows.getFirst());
    }

    private static DrillUsage usage(LocalDate day, Object[] row) {
        return new DrillUsage(day, ((Number) row[0]).intValue(), ((Number) row[1]).intValue());
    }
}
