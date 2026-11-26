package com.recallio.review.infrastructure;

import com.recallio.review.domain.drill.DrillActivityRepository;
import com.recallio.review.domain.drill.DrillCheckId;
import com.recallio.review.domain.drill.DrillUsage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaDrillActivityRepository implements DrillActivityRepository {

    private static final String USER_ID = "userId";
    private static final String DAY = "day";
    private static final String CHECK_ID = "checkId";

    private final EntityManager entityManager;

    public JpaDrillActivityRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * {@code ON CONFLICT DO UPDATE ... WHERE} : la ligne du jour est verrouillée, puis la condition relue. Deux réponses
     * simultanées comptent toutes les deux, sans lecture préalable, et jamais au-delà de la limite. Les empreintes les
     * plus récentes sont gardées, les autres oubliées.
     */
    @Override
    public Optional<DrillUsage> record(UUID userId, LocalDate day, boolean correct, UUID checkId, int dailyLimit) {
        List<?> counted = entityManager
                .createNativeQuery("""
                        insert into drill_activity (user_id, local_date, answers, correct, recent_checks)
                        values (:userId, :day, 1, :correct, array[cast(:checkId as uuid)])
                        on conflict (user_id, local_date) do update
                        set answers = drill_activity.answers + 1,
                            correct = drill_activity.correct + excluded.correct,
                            recent_checks = (excluded.recent_checks || drill_activity.recent_checks)[1:%d]
                        where drill_activity.answers < :dailyLimit
                          and not (cast(:checkId as uuid) = any (drill_activity.recent_checks))
                        returning answers, correct
                        """.formatted(DrillCheckId.KEPT_PER_DAY))
                .setParameter(USER_ID, userId)
                .setParameter(DAY, day)
                .setParameter("correct", correct ? 1 : 0)
                .setParameter(CHECK_ID, checkId)
                .setParameter("dailyLimit", dailyLimit)
                .getResultList();
        if (!counted.isEmpty()) {
            return Optional.of(usage(day, (Object[]) counted.getFirst()));
        }
        // Rien n'a été compté : la réponse l'était déjà (une requête rejouée), ou la limite du jour est atteinte.
        List<?> replayed = entityManager
                .createNativeQuery("""
                        select answers, correct from drill_activity
                        where user_id = :userId and local_date = :day
                          and cast(:checkId as uuid) = any (recent_checks)
                        """)
                .setParameter(USER_ID, userId)
                .setParameter(DAY, day)
                .setParameter(CHECK_ID, checkId)
                .getResultList();
        return replayed.stream().findFirst().map(row -> usage(day, (Object[]) row));
    }

    @Override
    public DrillUsage find(UUID userId, LocalDate day) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select answers, correct from drill_activity
                        where user_id = :userId and local_date = :day
                        """)
                .setParameter(USER_ID, userId)
                .setParameter(DAY, day)
                .getResultList();
        return rows.isEmpty() ? DrillUsage.none(day) : usage(day, (Object[]) rows.getFirst());
    }

    private static DrillUsage usage(LocalDate day, Object[] row) {
        return new DrillUsage(day, ((Number) row[0]).intValue(), ((Number) row[1]).intValue());
    }
}
