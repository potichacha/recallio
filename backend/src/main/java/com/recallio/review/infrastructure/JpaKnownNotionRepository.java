package com.recallio.review.infrastructure;

import com.recallio.review.domain.level.KnownNotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaKnownNotionRepository implements KnownNotionRepository {

    private final EntityManager entityManager;

    public JpaKnownNotionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO NOTHING} : refaire le test n'enlève rien, et la première date reste. */
    @Override
    public void markKnown(UUID userId, String subjectSlug, Collection<UUID> notionIds, Instant knownAt) {
        if (notionIds.isEmpty()) {
            return;
        }
        entityManager
                .createNativeQuery("""
                        insert into known_notion (user_id, notion_id, subject_slug, known_at)
                        select :userId, notion_id, :subject, :knownAt
                        from unnest(cast(:notionIds as uuid[])) as notion_id
                        on conflict (user_id, notion_id) do nothing
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setParameter("knownAt", knownAt)
                .setParameter("notionIds", notionIds.toArray(UUID[]::new))
                .executeUpdate();
    }

    @Override
    public Set<UUID> known(UUID userId, Collection<String> subjectSlugs) {
        if (subjectSlugs.isEmpty()) {
            return Set.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        select notion_id from known_notion
                        where user_id = :userId and subject_slug = any (cast(:subjects as text[]))
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("subjects", subjectSlugs.toArray(String[]::new))
                .getResultList();
        return rows.stream().map(UUID.class::cast).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void forget(UUID userId, String subjectSlug) {
        entityManager
                .createNativeQuery("delete from known_notion where user_id = :userId and subject_slug = :subject")
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .executeUpdate();
    }
}
