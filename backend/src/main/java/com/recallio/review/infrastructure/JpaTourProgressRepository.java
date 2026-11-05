package com.recallio.review.infrastructure;

import com.recallio.review.domain.tour.TourLastPage;
import com.recallio.review.domain.tour.TourProgressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaTourProgressRepository implements TourProgressRepository {

    private final EntityManager entityManager;

    public JpaTourProgressRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO NOTHING} : la date de la première validation reste. */
    @Override
    public boolean markValidated(UUID userId, UUID notionId, Instant validatedAt) {
        return entityManager
                        .createNativeQuery("""
                        insert into tour_progress (user_id, notion_id, validated_at)
                        values (:userId, :notionId, :validatedAt)
                        on conflict (user_id, notion_id) do nothing
                        """)
                        .setParameter("userId", userId)
                        .setParameter("notionId", notionId)
                        .setParameter("validatedAt", validatedAt)
                        .executeUpdate()
                > 0;
    }

    @Override
    public long validatedCount(UUID userId) {
        return ((Number) entityManager
                        .createNativeQuery("select count(*) from tour_progress where user_id = :userId")
                        .setParameter("userId", userId)
                        .getSingleResult())
                .longValue();
    }

    @Override
    public Set<UUID> validated(UUID userId, Collection<UUID> notionIds) {
        if (notionIds.isEmpty()) {
            return Set.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        select notion_id from tour_progress
                        where user_id = :userId and notion_id = any (cast(:notionIds as uuid[]))
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("notionIds", notionIds.toArray(UUID[]::new))
                .getResultList();
        return rows.stream().map(UUID.class::cast).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void saveLastPage(UUID userId, String subjectSlug, TourLastPage lastPage, Instant openedAt) {
        entityManager
                .createNativeQuery("""
                        insert into tour_last_page (user_id, subject_slug, notion_id, page, opened_at)
                        values (:userId, :subject, :notionId, :page, :openedAt)
                        on conflict (user_id, subject_slug) do update
                        set notion_id = excluded.notion_id, page = excluded.page, opened_at = excluded.opened_at
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setParameter("notionId", lastPage.notionId())
                .setParameter("page", lastPage.page())
                .setParameter("openedAt", openedAt)
                .executeUpdate();
    }

    @Override
    public Optional<TourLastPage> lastPage(UUID userId, String subjectSlug) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select notion_id, page from tour_last_page
                        where user_id = :userId and subject_slug = :subject
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new TourLastPage((UUID) row[0], ((Number) row[1]).intValue()))
                .findFirst();
    }
}
