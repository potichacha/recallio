package com.recallio.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.platform.domain.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Le plan du site (ADR 0065) : les pages publiques des sujets publiés, dans l'ordre du catalogue. */
class SitemapServiceTest {

    private static final List<String> FIXED =
            List.of("/", "/learn", "/tours", "/subjects", "/mentions-legales", "/confidentialite");

    private final FakeCatalog catalog = new FakeCatalog();
    private final SitemapService sitemap = new SitemapService(catalog);

    @Test
    void shouldListTheFixedPublicPagesEvenWithoutSubject() {
        assertThat(sitemap.paths()).containsExactlyElementsOf(FIXED);
    }

    @Test
    void shouldListAPublishedSubjectThenItsLessonsThenItsTour() {
        catalog.add("go", Subject.Status.PUBLISHED, List.of("hello-world", "boucle-for"), List.of("hello-world"));

        assertThat(sitemap.paths())
                .containsExactly(
                        "/",
                        "/learn",
                        "/tours",
                        "/subjects",
                        "/mentions-legales",
                        "/confidentialite",
                        "/subjects/go",
                        "/subjects/go/course/hello-world",
                        "/subjects/go/course/boucle-for",
                        "/subjects/go/tour",
                        "/subjects/go/tour/hello-world");
    }

    @Test
    void shouldGiveNoTourPageToASubjectWithoutATour() {
        catalog.add("sql", Subject.Status.PUBLISHED, List.of("jointures"), List.of());

        assertThat(sitemap.paths())
                .endsWith("/subjects/sql", "/subjects/sql/course/jointures")
                .noneMatch(path -> path.startsWith("/subjects/sql/tour"));
    }

    @Test
    void shouldNeverListADraftSubjectEvenAnnounced() {
        catalog.add("rust", Subject.Status.DRAFT, List.of("ownership"), List.of("hello-world"));
        catalog.add("go", Subject.Status.PUBLISHED, List.of(), List.of());

        assertThat(sitemap.paths()).contains("/subjects/go").noneMatch(path -> path.startsWith("/subjects/rust"));
    }

    @Test
    void shouldEncodeEachSegmentOfAPathLikeThePwa() {
        catalog.add("go", Subject.Status.PUBLISHED, List.of("à la ligne"), List.of());

        assertThat(sitemap.paths()).contains("/subjects/go/course/%C3%A0%20la%20ligne");
    }

    /** Faux catalogue : il ne connaît que ce que lit le plan du site, et filtre les brouillons comme la base. */
    private static final class FakeCatalog implements ContentCatalog {

        private final List<SubjectSummary> subjects = new ArrayList<>();
        private final Map<UUID, List<String>> lessons = new HashMap<>();
        private final Map<UUID, List<TourEntry>> tours = new HashMap<>();

        void add(String slug, Subject.Status status, List<String> lessonNotions, List<String> tourNotions) {
            UUID id = UUID.randomUUID();
            subjects.add(new SubjectSummary(
                    id,
                    slug,
                    slug,
                    Subject.Category.LANGUAGES,
                    10 * (subjects.size() + 1),
                    status,
                    1,
                    1,
                    lessonNotions.size(),
                    0,
                    !tourNotions.isEmpty()));
            lessons.put(id, List.copyOf(lessonNotions));
            tours.put(
                    id,
                    tourNotions.stream()
                            .map(notion -> new TourEntry(notion, "bases", "Les bases", "premiers-pas", "", notion, 1))
                            .toList());
        }

        @Override
        public List<SubjectSummary> subjects(Visibility visibility) {
            return subjects.stream()
                    .filter(subject ->
                            visibility == Visibility.INCLUDING_DRAFTS || subject.status() == Subject.Status.PUBLISHED)
                    .toList();
        }

        @Override
        public List<String> lessonNotions(UUID subjectId) {
            return lessons.getOrDefault(subjectId, List.of());
        }

        @Override
        public List<TourEntry> tour(UUID subjectId) {
            return tours.getOrDefault(subjectId, List.of());
        }

        @Override
        public Optional<SubjectSummary> subjectBySlug(String slug, Visibility visibility) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ThemeSummary> themes(UUID subjectId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ThemeSummary> themeBySlug(String slug, Visibility visibility) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<CardView> activeCards(UUID themeId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TourNotionPages> tourNotion(UUID subjectId, String notionSlug) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TourExercise> tourExercises(UUID subjectId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<NotionLesson> notionLesson(UUID subjectId, String notionSlug) {
            throw new UnsupportedOperationException();
        }
    }
}
