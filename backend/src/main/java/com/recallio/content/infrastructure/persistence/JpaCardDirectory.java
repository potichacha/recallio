package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardType;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.ThemeNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Toutes les requêtes ne voient que les sujets publiés : un brouillon n'est jamais proposé en révision. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class JpaCardDirectory implements CardDirectory {

    /** Condition JPQL ; chaque requête qui l'utilise lie le paramètre {@code published}. */
    private static final String PUBLISHED = "s.status = :published";

    private final EntityManager entityManager;

    public JpaCardDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<UUID> activeCardIds(CardFilter filter) {
        List<String> conditions = new ArrayList<>(List.of("c.retiredAt is null", PUBLISHED));
        String themeSlug = filter.themeSlug();
        CardType type = filter.type();
        if (themeSlug != null) {
            requireTheme(themeSlug);
            conditions.add("t.slug = :slug");
        }
        if (type != null) {
            conditions.add("c.type = :type");
        }
        var query = entityManager.createQuery("""
                select c.id from CardEntity c
                join ThemeEntity t on t.id = c.themeId
                join SubjectEntity s on s.id = t.subjectId
                where %s
                order by s.position, t.position, c.position
                """.formatted(String.join(" and ", conditions)), UUID.class);
        query.setParameter("published", Subject.Status.PUBLISHED);
        if (themeSlug != null) {
            query.setParameter("slug", themeSlug);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getResultList();
    }

    @Override
    public List<CardSnapshot> currentVersions(Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, CardSnapshot> found = entityManager
                .createQuery("""
                        select c.id, v.id, c.type, s.slug, s.title, t.slug, t.title, v.prompt, v.answer
                        from CardEntity c
                        join CardVersionEntity v on v.cardId = c.id and v.version = c.currentVersion
                        join ThemeEntity t on t.id = c.themeId
                        join SubjectEntity s on s.id = t.subjectId
                        where c.id in :ids and c.retiredAt is null and %s
                        """.formatted(PUBLISHED), Object[].class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .setParameter("ids", cardIds)
                .getResultStream()
                .map(row -> new CardSnapshot(
                        (UUID) row[0],
                        (UUID) row[1],
                        (CardType) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (String) row[7],
                        (String) row[8]))
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        return cardIds.stream()
                .distinct()
                .flatMap(id -> Stream.ofNullable(found.get(id)))
                .toList();
    }

    @Override
    public List<ThemeCards> activeCardsByTheme() {
        Map<UUID, List<UUID>> cardsByTheme = new HashMap<>();
        entityManager
                .createQuery(
                        "select c.themeId, c.id from CardEntity c where c.retiredAt is null order by c.position",
                        Object[].class)
                .getResultStream()
                .forEach(row -> cardsByTheme
                        .computeIfAbsent((UUID) row[0], _ -> new ArrayList<>())
                        .add((UUID) row[1]));
        return entityManager
                .createQuery("""
                        select t.id, s.slug, s.title, t.slug, t.title
                        from ThemeEntity t
                        join SubjectEntity s on s.id = t.subjectId
                        where %s
                        order by s.position, t.position
                        """.formatted(PUBLISHED), Object[].class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultStream()
                .map(row -> new ThemeCards(
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        cardsByTheme.getOrDefault((UUID) row[0], List.of())))
                .toList();
    }

    private void requireTheme(String slug) {
        boolean exists = !entityManager
                .createQuery("""
                        select t.id from ThemeEntity t
                        join SubjectEntity s on s.id = t.subjectId
                        where t.slug = :slug and %s
                        """.formatted(PUBLISHED), UUID.class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .setParameter("slug", slug)
                .getResultList()
                .isEmpty();
        if (!exists) {
            throw new ThemeNotFoundException(slug);
        }
    }
}
