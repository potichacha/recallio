package com.recallio.content.infrastructure.persistence;

import com.recallio.content.SubjectDirectory;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.SubjectNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class JpaSubjectDirectory implements SubjectDirectory {

    private final EntityManager entityManager;

    public JpaSubjectDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<String> publishedSubjectSlugs() {
        return entityManager
                .createQuery(
                        "select s.slug from SubjectEntity s where s.status = :published order by s.position, s.slug",
                        String.class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultList();
    }

    @Override
    public void requirePublished(String slug) {
        if (!publishedSubjectSlugs().contains(slug)) {
            throw new SubjectNotFoundException(slug);
        }
    }
}
