package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class JpaContentCatalog implements ContentCatalog {

    private static final String SUBJECT_SUMMARIES = """
            select s.id, s.slug, s.title, s.category, s.position, s.status, count(distinct t.id), count(c.id)
            from SubjectEntity s
            left join ThemeEntity t on t.subjectId = s.id and t.retiredAt is null
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            where s.status in :statuses %s
            group by s.id, s.slug, s.title, s.category, s.position, s.status
            order by s.position, s.slug
            """;

    /**
     * Ce que le résumé d'un sujet dit de ses notions et de ses exercices : combien il a de notions actives, combien
     * ont des pages dans le Tour (ADR 0037), et combien il a d'exercices de code.
     */
    private static final String SUBJECT_FACTS = """
            select s.slug,
                   (select count(*) from notion n where n.subject_id = s.id and n.retired_at is null),
                   (select count(*) from notion n
                    where n.subject_id = s.id and n.retired_at is null and jsonb_array_length(n.tour) > 0),
                   (select count(*) from card c join theme t on t.id = c.theme_id
                    where t.subject_id = s.id and c.retired_at is null and c.type = 'CODING_EXERCISE')
            from subject s
            """;

    /** @param tourNotions le sujet a un Tour dès qu'une de ses notions en a des pages */
    private record SubjectFacts(long notions, long tourNotions, long exercises) {
        static final SubjectFacts NONE = new SubjectFacts(0, 0, 0);
    }

    private static final String THEME_SUMMARIES = """
            select t.id, s.slug, s.title, t.slug, t.title, t.position, count(c.id)
            from ThemeEntity t
            join SubjectEntity s on s.id = t.subjectId
            left join CardEntity c on c.themeId = t.id and c.retiredAt is null
            where t.retiredAt is null and %s
            group by t.id, s.slug, s.title, t.slug, t.title, t.position
            order by t.position
            """;

    /**
     * Le sommaire du Tour, sans lire le contenu des pages : le titre de la première et leur nombre (ADR 0037). Le titre
     * d'un domaine est celui de son thème ({@code <sujet>-<domaine>}).
     */
    private static final String TOUR_ENTRIES = """
            select n.slug, n.domain, coalesce(t.title, n.domain), n.chapter, n.chapter_title,
                   coalesce(n.tour -> 0 ->> 'title', n.title), jsonb_array_length(n.tour)
            from notion n
            join subject s on s.id = n.subject_id
            left join theme t on t.subject_id = n.subject_id
                and t.slug = s.slug || '-' || n.domain
                and t.retired_at is null
            where n.subject_id = :subjectId and n.retired_at is null and jsonb_array_length(n.tour) > 0
            order by n.position
            """;

    private final EntityManager entityManager;

    public JpaContentCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SubjectSummary> subjects(Visibility visibility) {
        Map<String, SubjectFacts> facts = subjectFacts();
        return entityManager
                .createQuery(SUBJECT_SUMMARIES.formatted(""), Object[].class)
                .setParameter("statuses", statuses(visibility))
                .getResultStream()
                .map(row -> toSubjectSummary(row, facts))
                .toList();
    }

    @Override
    public Optional<SubjectSummary> subjectBySlug(String slug, Visibility visibility) {
        return entityManager
                .createQuery(SUBJECT_SUMMARIES.formatted("and s.slug = :slug"), Object[].class)
                .setParameter("statuses", statuses(visibility))
                .setParameter("slug", slug)
                .getResultStream()
                .map(row -> toSubjectSummary(row, subjectFacts()))
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
                        select c.id, c.type, v.prompt, v.answer, v.choices, v.version, v.blanks, v.exercise
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
                        (UUID) row[0],
                        (CardType) row[1],
                        (String) row[2],
                        (String) row[3],
                        JpaCardDirectory.choices(row[4]),
                        JpaCardDirectory.blanks(row[6]),
                        (ExerciseFiles) row[7],
                        (Integer) row[5]))
                .toList();
        return new Page<>(cards, page, size, total);
    }

    @Override
    public List<TourEntry> tour(UUID subjectId) {
        List<?> rows = entityManager
                .createNativeQuery(TOUR_ENTRIES)
                .setParameter("subjectId", subjectId)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new TourEntry(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        ((Number) row[6]).intValue()))
                .toList();
    }

    @Override
    public List<TourExercise> tourExercises(UUID subjectId) {
        return entityManager
                .createQuery("""
                        select n.domain, n.chapter, n.slug, c.id, v.prompt
                        from CardEntity c
                        join NotionEntity n on n.id = c.notionId
                        join CardVersionEntity v on v.cardId = c.id and v.version = c.currentVersion
                        where n.subjectId = :subjectId and n.retiredAt is null and c.retiredAt is null
                          and c.type = :exercise
                        order by n.position, c.position
                        """, Object[].class)
                .setParameter("subjectId", subjectId)
                .setParameter("exercise", CardType.CODING_EXERCISE)
                .getResultStream()
                .map(row -> new TourExercise(
                        (String) row[0], (String) row[1], (String) row[2], (UUID) row[3], TourExercise.summary((String)
                                row[4])))
                .toList();
    }

    @Override
    public Optional<TourNotionPages> tourNotion(UUID subjectId, String notionSlug) {
        return entityManager
                .createQuery(
                        "select n from NotionEntity n where n.subjectId = :subjectId and n.slug = :slug"
                                + " and n.retiredAt is null",
                        NotionEntity.class)
                .setParameter("subjectId", subjectId)
                .setParameter("slug", notionSlug)
                .getResultStream()
                .filter(notion -> !notion.tour.isEmpty())
                .map(notion -> new TourNotionPages(notion.slug, notion.title, notion.chapterTitle, notion.tour))
                .findFirst();
    }

    @Override
    public Optional<NotionLesson> notionLesson(UUID subjectId, String notionSlug) {
        return entityManager
                .createQuery(
                        "select n from NotionEntity n where n.subjectId = :subjectId and n.slug = :slug"
                                + " and n.retiredAt is null",
                        NotionEntity.class)
                .setParameter("subjectId", subjectId)
                .setParameter("slug", notionSlug)
                .getResultStream()
                .findFirst()
                .map(notion -> new NotionLesson(
                        notion.slug,
                        notion.title,
                        notion.chapterTitle,
                        notion.lesson,
                        !notion.tour.isEmpty(),
                        neighbour(subjectId, notion.position, false),
                        neighbour(subjectId, notion.position, true)));
    }

    /** La notion active la plus proche avant ({@code after} faux) ou après cette position, dans le référentiel. */
    private @Nullable NotionLink neighbour(UUID subjectId, int position, boolean after) {
        String query = after
                ? "select n.slug, n.title from NotionEntity n where n.subjectId = :subjectId and n.retiredAt is null"
                        + " and n.position > :position order by n.position"
                : "select n.slug, n.title from NotionEntity n where n.subjectId = :subjectId and n.retiredAt is null"
                        + " and n.position < :position order by n.position desc";
        return entityManager
                .createQuery(query, Object[].class)
                .setParameter("subjectId", subjectId)
                .setParameter("position", position)
                .setMaxResults(1)
                .getResultStream()
                .map(row -> new NotionLink((String) row[0], (String) row[1]))
                .findFirst()
                .orElse(null);
    }

    private static List<Subject.Status> statuses(Visibility visibility) {
        return switch (visibility) {
            case PUBLISHED_ONLY -> List.of(Subject.Status.PUBLISHED);
            case INCLUDING_DRAFTS -> List.of(Subject.Status.values());
        };
    }

    private Map<String, SubjectFacts> subjectFacts() {
        List<?> rows = entityManager.createNativeQuery(SUBJECT_FACTS).getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> new SubjectFacts(
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).longValue())));
    }

    private static SubjectSummary toSubjectSummary(Object[] row, Map<String, SubjectFacts> facts) {
        String slug = (String) row[1];
        SubjectFacts fact = facts.getOrDefault(slug, SubjectFacts.NONE);
        return new SubjectSummary(
                (UUID) row[0],
                slug,
                (String) row[2],
                (Subject.Category) row[3],
                (Integer) row[4],
                (Subject.Status) row[5],
                (Long) row[6],
                (Long) row[7],
                fact.notions(),
                fact.exercises(),
                fact.tourNotions() > 0);
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
