package com.recallio.content.infrastructure.persistence;

import com.recallio.content.TourDirectory;
import com.recallio.content.TourPage;
import com.recallio.content.domain.Subject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaTourDirectory implements TourDirectory {

    private final EntityManager entityManager;

    public JpaTourDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<TourPage> page(String subjectSlug, String notionSlug, int page) {
        if (page < 1) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("""
                        select n from NotionEntity n join SubjectEntity s on s.id = n.subjectId
                        where s.slug = :subject and n.slug = :notion and n.retiredAt is null and s.status = :published
                        """, NotionEntity.class)
                .setParameter("subject", subjectSlug)
                .setParameter("notion", notionSlug)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultStream()
                .findFirst()
                .map(notion -> notion.tour)
                .filter(pages -> page <= pages.size())
                .map((List<TourPage> pages) -> pages.get(page - 1));
    }
}
