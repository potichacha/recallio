package com.recallio.review.infrastructure;

import com.recallio.review.domain.tour.TourProgressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
    public void markValidated(UUID userId, UUID notionId, Instant validatedAt) {
        entityManager
                .createNativeQuery("""
                        insert into tour_progress (user_id, notion_id, validated_at)
                        values (:userId, :notionId, :validatedAt)
                        on conflict (user_id, notion_id) do nothing
                        """)
                .setParameter("userId", userId)
                .setParameter("notionId", notionId)
                .setParameter("validatedAt", validatedAt)
                .executeUpdate();
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
}
