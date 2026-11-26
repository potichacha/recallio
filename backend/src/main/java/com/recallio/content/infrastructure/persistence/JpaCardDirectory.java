package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter.Grading;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectScope;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.TourQuestions;
import com.recallio.content.domain.catalog.ThemeNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Les requêtes ne voient que les sujets publiés : un brouillon n'est jamais proposé en révision. Seules celles qui
 * prennent un {@link SubjectScope} peuvent en montrer, à un admin.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class JpaCardDirectory implements CardDirectory {

    /** Condition JPQL ; chaque requête qui l'utilise lie le paramètre {@code published}. */
    private static final String PUBLISHED = "s.status = :published";

    /**
     * Le thème est sélectionné, ou son sujet n'a aucun thème sélectionné (il les garde alors tous). Lie le paramètre
     * {@code selection}.
     */
    private static final String THEME_SELECTION = """
            (t.slug in :selection
             or not exists (select 1 from ThemeEntity other where other.subjectId = s.id and other.retiredAt is null
                    and other.slug in :selection))
            """;

    /** La version courante de la carte a des choix (ADR 0051). */
    private static final String HAS_CHOICES = """
            exists (select 1 from CardVersionEntity v
                    where v.cardId = c.id and v.version = c.currentVersion and v.choices is not null)
            """;

    private final EntityManager entityManager;

    public JpaCardDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ActiveCard> activeCards(CardFilter filter) {
        Set<String> subjectSlugs = filter.subjectSlugs();
        if (subjectSlugs != null && subjectSlugs.isEmpty()) {
            return List.of();
        }
        List<String> conditions = new ArrayList<>(List.of("c.retiredAt is null", PUBLISHED));
        Map<String, Object> parameters = new HashMap<>(Map.of("published", Subject.Status.PUBLISHED));
        if (subjectSlugs != null) {
            conditions.add("s.slug in :subjects");
            parameters.put("subjects", subjectSlugs);
        }
        String themeSlug = filter.themeSlug();
        if (themeSlug != null) {
            requireTheme(themeSlug);
            conditions.add("t.slug = :slug");
            parameters.put("slug", themeSlug);
        }
        if (!filter.themeSelection().isEmpty()) {
            conditions.add(THEME_SELECTION);
            parameters.put("selection", filter.themeSelection());
        }
        if (!filter.types().isEmpty()) {
            conditions.add("c.type in :types");
            parameters.put("types", filter.types());
        }
        addGrading(filter.grading(), conditions, parameters);
        var query = entityManager.createQuery("""
                select c.id, s.slug, c.notionId, c.type from CardEntity c
                join ThemeEntity t on t.id = c.themeId
                join SubjectEntity s on s.id = t.subjectId
                where %s
                order by s.position, t.position, c.position
                """.formatted(String.join(" and ", conditions)), Object[].class);
        parameters.forEach(query::setParameter);
        return query.getResultStream()
                .map(row -> new ActiveCard((UUID) row[0], (String) row[1], (UUID) row[2], (CardType) row[3]))
                .toList();
    }

    /**
     * Le critère de correction (ADR 0063) : des choix dans la version courante, ou un format que le serveur corrige
     * sans eux. Lie le paramètre {@code gradedWithoutChoices} quand il en faut un.
     */
    private static void addGrading(Grading grading, List<String> conditions, Map<String, Object> parameters) {
        switch (grading) {
            case ANY -> {}
            case CHOICES -> conditions.add(HAS_CHOICES);
            case CHOICES_OR_BLANKS -> addChoicesOr(Set.of(CardType.FILL_IN), conditions, parameters);
            case SERVER -> addChoicesOr(Set.of(CardType.FILL_IN, CardType.CODING_EXERCISE), conditions, parameters);
        }
    }

    /** Des choix, ou l'un de ces formats, que le serveur corrige sans eux. */
    private static void addChoicesOr(Set<CardType> formats, List<String> conditions, Map<String, Object> parameters) {
        conditions.add("(c.type in :gradedWithoutChoices or " + HAS_CHOICES + ")");
        parameters.put("gradedWithoutChoices", formats);
    }

    @Override
    public List<NotionInfo> notions(Collection<String> subjectSlugs) {
        if (subjectSlugs.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> cardCounts = new HashMap<>();
        entityManager
                .createQuery(
                        "select c.notionId, count(c) from CardEntity c where c.notionId is not null and c.retiredAt is null"
                                + " group by c.notionId",
                        Object[].class)
                .getResultStream()
                .forEach(row -> cardCounts.put((UUID) row[0], (Long) row[1]));
        Map<UUID, String> themeTitles = themeTitles(subjectSlugs);
        return entityManager
                .createQuery("""
                        select n, s.slug from NotionEntity n join SubjectEntity s on s.id = n.subjectId
                        where s.slug in :subjects and n.retiredAt is null and %s
                        order by s.position, n.position
                        """.formatted(PUBLISHED), Object[].class)
                .setParameter("subjects", subjectSlugs)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultStream()
                .map(row -> notionInfo((NotionEntity) row[0], (String) row[1], cardCounts, themeTitles))
                .toList();
    }

    /** Le titre des thèmes de ces sujets : le domaine d'un référentiel est importé comme un thème (ADR 0026). */
    private Map<UUID, String> themeTitles(Collection<String> subjectSlugs) {
        return entityManager
                .createQuery(
                        "select t.id, t.title from ThemeEntity t join SubjectEntity s on s.id = t.subjectId"
                                + " where s.slug in :subjects",
                        Object[].class)
                .setParameter("subjects", subjectSlugs)
                .getResultStream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (String) row[1]));
    }

    /** Un domaine sans carte n'a pas de thème : son identifiant tient lieu de titre. */
    private static NotionInfo notionInfo(
            NotionEntity notion, String subject, Map<UUID, Long> cardCounts, Map<UUID, String> themeTitles) {
        return new NotionInfo(
                notion.id,
                subject,
                notion.slug,
                notion.title,
                notion.domain,
                themeTitles.getOrDefault(CardIdentity.themeId(subject, notion.domain), notion.domain),
                notion.chapter,
                notion.chapterTitle,
                NotionInfo.Level.valueOf(notion.level.name()),
                notion.priority,
                Arrays.stream(notion.requires)
                        .map(slug -> CardIdentity.notionId(subject, slug))
                        .toList(),
                notion.position,
                cardCounts.getOrDefault(notion.id, 0L).intValue(),
                notion.tour.size());
    }

    /** Une seule notion, sans lire toutes celles du sujet : le Tour et l'entraînement en cherchent une à chaque appel. */
    @Override
    public Optional<NotionInfo> notion(String subjectSlug, String notionSlug) {
        return entityManager
                .createQuery("""
                        select n from NotionEntity n join SubjectEntity s on s.id = n.subjectId
                        where s.slug = :subject and n.slug = :notion and n.retiredAt is null and %s
                        """.formatted(PUBLISHED), NotionEntity.class)
                .setParameter("subject", subjectSlug)
                .setParameter("notion", notionSlug)
                .setParameter("published", Subject.Status.PUBLISHED)
                .getResultStream()
                .findFirst()
                .map(notion -> notionInfo(
                        notion,
                        subjectSlug,
                        Map.of(notion.id, activeCardCount(notion.id)),
                        themeTitles(Set.of(subjectSlug))));
    }

    private long activeCardCount(UUID notionId) {
        return entityManager
                .createQuery(
                        "select count(c) from CardEntity c where c.notionId = :notion and c.retiredAt is null",
                        Long.class)
                .setParameter("notion", notionId)
                .getSingleResult();
    }

    @Override
    public Map<UUID, UUID> notionsOf(Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return entityManager
                .createQuery(
                        "select c.id, c.notionId from CardEntity c where c.id in :ids and c.notionId is not null",
                        Object[].class)
                .setParameter("ids", cardIds)
                .getResultStream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (UUID) row[1]));
    }

    @Override
    public Map<UUID, String> subjectsOf(Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return entityManager
                .createQuery("""
                        select c.id, s.slug
                        from CardEntity c
                        join ThemeEntity t on t.id = c.themeId
                        join SubjectEntity s on s.id = t.subjectId
                        where c.id in :ids
                        """, Object[].class)
                .setParameter("ids", cardIds)
                .getResultStream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (String) row[1]));
    }

    @Override
    public List<CardSnapshot> currentVersions(Collection<UUID> cardIds) {
        return currentVersions(cardIds, SubjectScope.PUBLISHED);
    }

    @Override
    public List<CardSnapshot> currentVersions(Collection<UUID> cardIds, SubjectScope scope) {
        if (cardIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, CardSnapshot> found = entityManager
                .createQuery("""
                        select c.id, v.id, c.type, s.slug, s.title, t.slug, t.title, v.prompt, v.answer, v.choices,
                               v.blanks, v.exercise, n.slug, n.title, n.lesson
                        from CardEntity c
                        join CardVersionEntity v on v.cardId = c.id and v.version = c.currentVersion
                        left join NotionEntity n on n.id = c.notionId
                        join ThemeEntity t on t.id = c.themeId
                        join SubjectEntity s on s.id = t.subjectId
                        where c.id in :ids and c.retiredAt is null and s.status in :statuses
                        """, Object[].class)
                .setParameter("statuses", statuses(scope))
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
                        (String) row[8],
                        choices(row[9]),
                        blanks(row[10]),
                        (ExerciseFiles) row[11],
                        (String) row[12],
                        (String) row[13],
                        (String) row[14]))
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        return cardIds.stream()
                .distinct()
                .flatMap(id -> Stream.ofNullable(found.get(id)))
                .toList();
    }

    @Override
    public List<LeveledCard> choiceCards(Collection<String> subjectSlugs, SubjectScope scope) {
        if (subjectSlugs.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("""
                        select c.id, s.slug, n.id, n.level, n.priority from CardEntity c
                        join NotionEntity n on n.id = c.notionId
                        join ThemeEntity t on t.id = c.themeId
                        join SubjectEntity s on s.id = t.subjectId
                        where c.retiredAt is null and n.retiredAt is null and s.slug in :subjects
                          and s.status in :statuses and c.type <> :exercise and %s
                        order by s.position, t.position, c.position
                        """.formatted(HAS_CHOICES), Object[].class)
                .setParameter("subjects", subjectSlugs)
                .setParameter("statuses", statuses(scope))
                .setParameter("exercise", CardType.CODING_EXERCISE)
                .getResultStream()
                .map(row -> new LeveledCard(
                        (UUID) row[0],
                        (String) row[1],
                        (UUID) row[2],
                        NotionInfo.Level.valueOf(((NotionLevel) row[3]).name()),
                        ((Number) row[4]).intValue()))
                .toList();
    }

    /** Les statuts des sujets que {@code scope} laisse voir. */
    private static Set<Subject.Status> statuses(SubjectScope scope) {
        return switch (scope) {
            case PUBLISHED -> Set.of(Subject.Status.PUBLISHED);
            case WITH_DRAFTS -> Set.of(Subject.Status.PUBLISHED, Subject.Status.DRAFT);
        };
    }

    /** Les questions se choisissent sur la version courante des cartes : c'est elle qui a, ou non, des choix. */
    @Override
    public List<CardSnapshot> tourQuestions(String subjectSlug, String notionSlug) {
        List<UUID> cardIds = entityManager
                .createQuery("""
                        select c.id from CardEntity c
                        join NotionEntity n on n.id = c.notionId
                        join SubjectEntity s on s.id = n.subjectId
                        join ThemeEntity t on t.id = c.themeId
                        where c.retiredAt is null and n.retiredAt is null and s.status = :published
                          and s.slug = :subject and n.slug = :notion and function('jsonb_array_length', n.tour) > 0
                        order by t.position, c.position
                        """, UUID.class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .setParameter("subject", subjectSlug)
                .setParameter("notion", notionSlug)
                .getResultList();
        return TourQuestions.pick(
                currentVersions(cardIds), CardSnapshot::type, CardSnapshot::isGradedWithoutRunningCode);
    }

    @Override
    public Set<UUID> cardsOfVersionsGradedByServer(Collection<UUID> versionIds) {
        if (versionIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(entityManager
                .createQuery("""
                        select distinct v.cardId from CardVersionEntity v join CardEntity c on c.id = v.cardId
                        where v.id in :versions and (v.choices is not null or c.type in :gradedByServer)
                        """, UUID.class)
                .setParameter("versions", versionIds)
                .setParameter("gradedByServer", CardType.GRADED_BY_SERVER)
                .getResultList());
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
                        where t.retiredAt is null and %s
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
                        where t.slug = :slug and t.retiredAt is null and %s
                        """.formatted(PUBLISHED), UUID.class)
                .setParameter("published", Subject.Status.PUBLISHED)
                .setParameter("slug", slug)
                .getResultList()
                .isEmpty();
        if (!exists) {
            throw new ThemeNotFoundException(slug);
        }
    }

    /** La colonne JSON {@code blanks}, lue par Hibernate dans le type de l'attribut ; absente hors code à trous. */
    @SuppressWarnings("unchecked")
    static List<CardBlank> blanks(@Nullable Object column) {
        return column == null ? List.of() : (List<CardBlank>) column;
    }

    /** La colonne JSON {@code choices}, lue par Hibernate dans le type de l'attribut ; absente sans choix. */
    @SuppressWarnings("unchecked")
    static List<CardChoice> choices(@Nullable Object column) {
        return column == null ? List.of() : (List<CardChoice>) column;
    }
}
