package com.recallio.content.infrastructure.persistence;

import com.recallio.content.SubjectDirectory;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.SubjectNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, String> publishedTitles() {
        Map<String, String> titles = new LinkedHashMap<>();
        entityManager
                .createQuery(
                        "select s.slug, s.title from SubjectEntity s where s.status = :published"
                                + " order by s.position, s.slug",
                        Object[].class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultList()
                .forEach(row -> titles.put((String) row[0], (String) row[1]));
        return titles;
    }

    @Override
    public void requirePublished(String slug) {
        if (!publishedSubjectSlugs().contains(slug)) {
            throw new SubjectNotFoundException(slug);
        }
    }

    @Override
    public List<String> themeSlugs(String subjectSlug) {
        requirePublished(subjectSlug);
        return entityManager
                .createQuery("""
                        select t.slug from ThemeEntity t
                        join SubjectEntity s on s.id = t.subjectId
                        where s.slug = :subject and t.retiredAt is null
                        order by t.position
                        """, String.class)
                .setParameter("subject", subjectSlug)
                .getResultList();
    }
}
