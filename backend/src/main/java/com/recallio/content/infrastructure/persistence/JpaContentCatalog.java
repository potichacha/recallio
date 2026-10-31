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

    private static final String THEME_SUMMARIES = """
            select t.id, s.slug, s.title, t.slug, t.title, t.position, count(c.id)
            from ThemeEntity t
            join SubjectEntity s on s.id = t.subjectId
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            where s.status = :published %s
            group by t.id, s.slug, s.title, s.position, t.slug, t.title, t.position
            order by s.position, t.position
            """;

    private final EntityManager entityManager;

    public JpaContentCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ThemeSummary> themes() {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted(""), Object[].class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultStream()
                .map(JpaContentCatalog::toThemeSummary)
                .toList();
    }

    @Override
    public Optional<ThemeSummary> themeBySlug(String slug) {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted("and t.slug = :slug"), Object[].class)
                .setParameter("published", Subject.Status.PUBLISHED)
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
