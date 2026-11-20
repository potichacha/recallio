package com.recallio.review.infrastructure;

import com.recallio.review.domain.interview.InterviewPlan;
import com.recallio.review.domain.interview.InterviewPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaInterviewPlanRepository implements InterviewPlanRepository {

    private final EntityManager entityManager;

    public JpaInterviewPlanRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO UPDATE} : une nouvelle date remplace tout le plan, ligne de départ comprise. */
    @Override
    public void save(UUID userId, InterviewPlan plan) {
        entityManager
                .createNativeQuery("""
                        insert into interview_plan (user_id, subject_slug, interview_on, planned_on, notions_to_acquire)
                        values (:userId, :subject, :interviewOn, :plannedOn, :notions)
                        on conflict (user_id, subject_slug) do update
                        set interview_on = excluded.interview_on, planned_on = excluded.planned_on,
                            notions_to_acquire = excluded.notions_to_acquire
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", plan.subjectSlug())
                .setParameter("interviewOn", plan.interviewOn())
                .setParameter("plannedOn", plan.plannedOn())
                .setParameter("notions", plan.notionsToAcquire())
                .executeUpdate();
    }

    @Override
    public Map<String, InterviewPlan> findAll(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select subject_slug, interview_on, planned_on, notions_to_acquire from interview_plan
                        where user_id = :userId
                        """)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new InterviewPlan(
                        (String) row[0], toLocalDate(row[1]), toLocalDate(row[2]), ((Number) row[3]).intValue()))
                .collect(Collectors.toUnmodifiableMap(InterviewPlan::subjectSlug, Function.identity()));
    }

    @Override
    public void delete(UUID userId, String subjectSlug) {
        entityManager
                .createNativeQuery("delete from interview_plan where user_id = :userId and subject_slug = :subject")
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .executeUpdate();
    }

    /** Selon le pilote, une colonne {@code date} arrive en {@link LocalDate} ou en {@link java.sql.Date}. */
    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
}
