package com.recallio.content.infrastructure.persistence;

import com.recallio.content.domain.CardType;
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
            select t.id, t.slug, t.title, t.position, count(c.id)
            from ThemeEntity t
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            %s
            group by t.id, t.slug, t.title, t.position
            order by t.position
            """;

    private final EntityManager entityManager;

    public JpaContentCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ThemeSummary> themes() {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted(""), Object[].class)
                .getResultStream()
                .map(JpaContentCatalog::toThemeSummary)
                .toList();
    }

    @Override
    public Optional<ThemeSummary> themeBySlug(String slug) {
        return entityManager
                .createQuery(THEME_SUMMARIES.formatted("where t.slug = :slug"), Object[].class)
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
        return new ThemeSummary((UUID) row[0], (String) row[1], (String) row[2], (Integer) row[3], (Long) row[4]);
    }
}
