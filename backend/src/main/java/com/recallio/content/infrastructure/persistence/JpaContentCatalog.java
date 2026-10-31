package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardType;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaContentCatalog implements ContentCatalog {

    private static final String SUBJECT_SUMMARIES = """
            select s.id, s.slug, s.title, s.category, s.position, s.status, count(distinct t.id), count(c.id)
            from SubjectEntity s
            left join ThemeEntity t on t.subjectId = s.id
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            where s.status in :statuses %s
            group by s.id, s.slug, s.title, s.category, s.position, s.status
            order by s.position, s.slug
            """;

    private static final String THEME_SUMMARIES = """
            select t.id, s.slug, s.title, t.slug, t.title, t.position, count(c.id)
            from ThemeEntity t
            join SubjectEntity s on s.id = t.subjectId
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            where %s
            group by t.id, s.slug, s.title, t.slug, t.title, t.position
            order by t.position
            """;

    private final EntityManager entityManager;

    public JpaContentCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SubjectSummary> subjects(Visibility visibility) {
        return entityManager
                .createQuery(SUBJECT_SUMMARIES.formatted(""), Object[].class)
                .setParameter("statuses", statuses(visibility))
                .getResultStream()
                .map(JpaContentCatalog::toSubjectSummary)
                .toList();
    }

    @Override
    public Optional<SubjectSummary> subjectBySlug(String slug, Visibility visibility) {
        return entityManager
                .createQuery(SUBJECT_SUMMARIES.formatted("and s.slug = :slug"), Object[].class)
                .setParameter("statuses", statuses(visibility))
                .setParameter("slug", slug)
                .getResultStream()
                .map(JpaContentCatalog::toSubjectSummary)
                .findFirst();
    }

    @Override
    public List<ThemeSummary> themes(UUID subjectId) {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted("s.id = :subjectId"), Object[].class)
                .setParameter("subjectId", subjectId)
                .getResultStream()
                .map(JpaContentCatalog::toThemeSummary)
                .toList();
    }

    @Override
    public Optional<ThemeSummary> themeBySlug(String slug, Visibility visibility) {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted("t.slug = :slug and s.status in :statuses"), Object[].class)
                .setParameter("statuses", statuses(visibility))
                .setParameter("slug", slug)
                .getResultStream()
                .map(JpaContentCatalog::toThemeSummary)
                .findFirst();
    }

    @Override
    public Page<CardView> activeCards(UUID themeId, int page, int size) {
        long total = entityManager
                .createQuery(
                        "select count(c) from CardEntity c where c.themeId = :themeId and c.retiredAt is null",
                        Long.class)
                .setParameter("themeId", themeId)
                .getSingleResult();
        List<CardView> cards = entityManager
                .createQuery("""
                        select c.id, c.type, v.prompt, v.answer, v.version
                        from CardEntity c
                        join CardVersionEntity v on v.cardId = c.id and v.version = c.currentVersion
                        where c.themeId = :themeId and c.retiredAt is null
                        order by c.position
                        """, Object[].class)
                .setParameter("themeId", themeId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultStream()
                .map(row -> new CardView(
                        (UUID) row[0], (CardType) row[1], (String) row[2], (String) row[3], (Integer) row[4]))
                .toList();
        return new Page<>(cards, page, size, total);
    }

    private static List<Subject.Status> statuses(Visibility visibility) {
        return switch (visibility) {
            case PUBLISHED_ONLY -> List.of(Subject.Status.PUBLISHED);
            case INCLUDING_DRAFTS -> List.of(Subject.Status.values());
        };
    }

    private static SubjectSummary toSubjectSummary(Object[] row) {
        return new SubjectSummary(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (Subject.Category) row[3],
                (Integer) row[4],
                (Subject.Status) row[5],
                (Long) row[6],
                (Long) row[7]);
    }

    private static ThemeSummary toThemeSummary(Object[] row) {
        return new ThemeSummary(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (Integer) row[5],
                (Long) row[6]);
    }
}
