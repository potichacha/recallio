package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.ActivityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Requêtes natives : le compteur doit être incrémenté en une seule instruction, sans lecture préalable. */
@ApplicationScoped
public class JpaActivityRepository implements ActivityRepository {

    private final EntityManager entityManager;

    public JpaActivityRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * {@code ON CONFLICT DO UPDATE} incrémente de façon atomique : deux révisions simultanées du même jour donnent bien
     * deux cartes, et {@code RETURNING} renvoie le compteur obtenu par cette révision-ci.
     */
    @Override
    public int recordReview(UUID userId, LocalDate day, int goal) {
        Number cardsReviewed = (Number) entityManager
                .createNativeQuery("""
                        insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                        values (:userId, :day, 1, 1 >= :goal)
                        on conflict (user_id, local_date) do update
                            set cards_reviewed = daily_activity.cards_reviewed + 1,
                                goal_reached = daily_activity.goal_reached
                                        or daily_activity.cards_reviewed + 1 >= :goal
                        returning cards_reviewed
                        """)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .setParameter("goal", goal)
                .getSingleResult();
        return cardsReviewed.intValue();
    }

    @Override
    public int cardsReviewed(UUID userId, LocalDate day) {
        List<?> rows = entityManager
                .createNativeQuery(
                        "select cards_reviewed from daily_activity where user_id = :userId and local_date = :day",
                        Integer.class)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .getResultList();
        return rows.isEmpty() ? 0 : ((Number) rows.getFirst()).intValue();
    }

    @Override
    public List<DailyActivity> between(UUID userId, LocalDate from, LocalDate to) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select local_date, cards_reviewed, goal_reached from daily_activity
                        where user_id = :userId and local_date between :from and :to
                        order by local_date
                        """)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new DailyActivity(toLocalDate(row[0]), ((Number) row[1]).intValue(), (Boolean) row[2]))
                .toList();
    }

    @Override
    public List<LocalDate> goalDays(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select local_date from daily_activity
                        where user_id = :userId and goal_reached
                        order by local_date desc
                        """, LocalDate.class)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream().map(LocalDate.class::cast).toList();
    }

    /** Selon le pilote, une colonne {@code date} arrive en {@link LocalDate} ou en {@link java.sql.Date}. */
    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
}
