package com.recallio.review.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.group.GroupReviewScopes.GroupReviewScope;
import com.recallio.review.application.ReviewQueueService.QueueFilter;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewLogEntry;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.fsrs.FsrsParameters;
import com.recallio.review.domain.tour.TourLastPage;
import com.recallio.review.domain.tour.TourProgressRepository;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
import com.recallio.user.UserRef;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReviewQueueServiceTest {

    private static final int NEW_CARDS_PER_SUBJECT_PER_DAY = 2;
    private static final String SUBJECT = "java";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final List<UUID> catalog =
            IntStream.range(0, 5).mapToObj(_ -> UUID.randomUUID()).toList();
    private final InMemoryStates states = new InMemoryStates();
    private final List<CardFilter> filters = new ArrayList<>();
    private final UUID groupId = UUID.randomUUID();
    private final GroupReviewScope groupScope =
            new GroupReviewScope(Set.of("java", "go"), Set.of("go-concurrence"), Set.of(CardType.FLASHCARD));
    private final UserRef user = new UserRef(UUID.randomUUID(), PARIS);

    @Test
    void shouldCountCardsIntroducedSinceMidnightInTheUserTimezone() {
        // 23 h 30 à Paris : la carte vue à 00 h 10 (heure de Paris) compte dans le quota du jour.
        Instant now = Instant.parse("2026-10-29T22:30:00Z");
        states.introduce(catalog.get(0), Instant.parse("2026-10-28T23:10:00Z"));

        var queue = serviceAt(now).queue(user, ReviewQueueService.QueueFilter.followedSubjects(), 10);

        assertThat(queue.newCount()).isEqualTo(1);
    }

    @Test
    void shouldResetTheNewCardQuotaAtMidnightInTheUserTimezone() {
        // 00 h 30 à Paris le lendemain, mais encore la veille en UTC : le quota est renouvelé.
        states.introduce(catalog.get(0), Instant.parse("2026-10-29T10:00:00Z"));
        states.introduce(catalog.get(1), Instant.parse("2026-10-29T11:00:00Z"));
        Instant justAfterMidnightInParis = Instant.parse("2026-10-29T23:30:00Z");

        var queue =
                serviceAt(justAfterMidnightInParis).queue(user, ReviewQueueService.QueueFilter.followedSubjects(), 10);

        assertThat(queue.newCount()).isEqualTo(NEW_CARDS_PER_SUBJECT_PER_DAY);
    }

    /** Finir sa session de Go ne prive pas Java de ses nouvelles cartes (ADR 0048). */
    @Test
    void shouldCountTheNewCardsOfEachSubjectAgainstItsOwnQuota() {
        Instant now = Instant.parse("2026-10-29T08:00:00Z");
        states.introduce(UUID.randomUUID(), now.minus(Duration.ofMinutes(20)));
        states.introduce(UUID.randomUUID(), now.minus(Duration.ofMinutes(10)));

        var queue = serviceAt(now).queue(user, new QueueFilter(null, SUBJECT, null, Set.of()), 10);

        assertThat(queue.newCount()).isEqualTo(NEW_CARDS_PER_SUBJECT_PER_DAY);
        assertThat(queue.cards()).extracting(card -> card.state()).containsOnly(LearningState.NEW);
    }

    @Test
    void shouldReviewOnlyTheRequestedFormats() {
        var requested = Set.of(CardType.MULTIPLE_CHOICE, CardType.TRUE_FALSE);

        serviceAt(Instant.parse("2026-10-29T08:00:00Z"))
                .queue(user, new QueueFilter(null, SUBJECT, null, requested), 10);

        assertThat(filters).containsExactly(new CardFilter(Set.of(SUBJECT), null, Set.of(), requested));
    }

    @Test
    void shouldReviewTheSubjectsThemesAndFormatsChosenByTheGroup() {
        serviceAt(Instant.parse("2026-10-29T08:00:00Z"))
                .queue(user, new QueueFilter(groupId, null, null, Set.of()), 10);

        assertThat(filters)
                .containsExactly(new CardFilter(
                        Set.of("java", "go"), null, Set.of("go-concurrence"), Set.of(CardType.FLASHCARD)));
    }

    @Test
    void shouldNarrowTheGroupCardsToARequestedSubject() {
        var service = serviceAt(Instant.parse("2026-10-29T08:00:00Z"));

        service.queue(user, new QueueFilter(groupId, "go", null, Set.of()), 10);
        var outside = service.queue(user, new QueueFilter(groupId, "rust", null, Set.of()), 10);

        assertThat(filters.get(0).subjectSlugs()).containsExactly("go");
        assertThat(filters.get(1).subjectSlugs()).isEmpty();
        assertThat(outside.cards()).isEmpty();
    }

    @Test
    void shouldKeepOnlyTheRequestedFormatsThatTheGroupReviews() {
        var service = serviceAt(Instant.parse("2026-10-29T08:00:00Z"));

        service.queue(user, new QueueFilter(groupId, null, null, Set.of(CardType.FLASHCARD, CardType.DEFINITION)), 10);
        var outside = service.queue(user, new QueueFilter(groupId, null, null, Set.of(CardType.DEFINITION)), 10);

        assertThat(filters.get(0).types()).containsExactly(CardType.FLASHCARD);
        assertThat(filters.get(1).subjectSlugs()).isEmpty();
        assertThat(outside.cards()).isEmpty();
    }

    private ReviewQueueService serviceAt(Instant now) {
        var directory = new FixedCatalog(catalog, filters);
        return new ReviewQueueService(
                directory,
                new SubjectDirectory() {
                    @Override
                    public List<String> publishedSubjectSlugs() {
                        return List.of(SUBJECT);
                    }

                    @Override
                    public Map<String, String> publishedTitles() {
                        return Map.of(SUBJECT, "Java");
                    }

                    @Override
                    public void requirePublished(String slug) {}

                    @Override
                    public List<String> themeSlugs(String subjectSlug) {
                        return List.of("java-theme");
                    }
                },
                new FollowingJava(),
                (_, _) -> groupScope,
                states,
                new NoReviewsToday(),
                new NoTour(),
                new NewCardQuota(states, directory, new TestConfig()),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private record TestConfig() implements ReviewConfig {
        @Override
        public int newCardsPerSubjectPerDay() {
            return NEW_CARDS_PER_SUBJECT_PER_DAY;
        }

        @Override
        public double desiredRetention() {
            return FsrsParameters.DEFAULT.desiredRetention();
        }
    }

    /**
     * Toujours les mêmes cartes Java, sauf pour un filtre qui n'admet aucun sujet ; garde les filtres reçus. Une carte
     * hors du catalogue est une carte de Go.
     */
    private record FixedCatalog(List<UUID> ids, List<CardFilter> filters) implements CardDirectory {
        @Override
        public List<ActiveCard> activeCards(CardFilter filter) {
            filters.add(filter);
            Set<String> subjects = filter.subjectSlugs();
            if (subjects != null && subjects.isEmpty()) {
                return List.of();
            }
            return ids.stream()
                    .map(id -> new ActiveCard(id, SUBJECT, null, CardType.FLASHCARD))
                    .toList();
        }

        @Override
        public Map<UUID, UUID> notionsOf(Collection<UUID> cardIds) {
            return Map.of();
        }

        @Override
        public Map<UUID, String> subjectsOf(Collection<UUID> cardIds) {
            Map<UUID, String> subjects = new HashMap<>();
            cardIds.forEach(id -> subjects.put(id, ids.contains(id) ? SUBJECT : "go"));
            return subjects;
        }

        @Override
        public List<NotionInfo> notions(Collection<String> subjectSlugs) {
            return List.of();
        }

        @Override
        public List<ThemeCards> activeCardsByTheme() {
            return List.of(new ThemeCards("java", "Java", "java-theme", "Thème", ids));
        }

        @Override
        public List<CardSnapshot> tourQuestions(String subjectSlug, String notionSlug) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CardSnapshot> currentVersions(Collection<UUID> cardIds) {
            return cardIds.stream()
                    .map(id -> new CardSnapshot(
                            id,
                            UUID.randomUUID(),
                            CardType.FLASHCARD,
                            "java",
                            "Java",
                            "java-theme",
                            "Thème",
                            "Question ?",
                            "Réponse.",
                            List.of(),
                            List.of(),
                            null,
                            null,
                            null,
                            null))
                    .toList();
        }
    }

    /** Un journal sans révision du jour : les cartes sœurs sont couvertes par ReviewQueuePlannerTest. */
    private static final class NoReviewsToday implements ReviewLogRepository {
        @Override
        public boolean appendIfAbsent(ReviewLogEntry entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ReviewLogEntry> findByRequestId(UUID requestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<UUID> cardsReviewedSince(UUID userId, Instant since) {
            return Set.of();
        }

        @Override
        public Set<UUID> cardsRecalled(UUID userId) {
            return Set.of();
        }

        @Override
        public List<UUID> cardsStillFailedSince(UUID userId, Instant since) {
            return List.of();
        }
    }

    /** Aucune notion validée dans le Tour : sans effet ici, le parcours par défaut ne s'en sert pas. */
    private static final class NoTour implements TourProgressRepository {
        @Override
        public boolean markValidated(UUID userId, UUID notionId, Instant validatedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long validatedCount(UUID userId) {
            return 0;
        }

        @Override
        public Set<UUID> validated(UUID userId, Collection<UUID> notionIds) {
            return Set.of();
        }

        @Override
        public void saveLastPage(UUID userId, String subjectSlug, TourLastPage lastPage, Instant openedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TourLastPage> lastPage(UUID userId, String subjectSlug) {
            return Optional.empty();
        }
    }

    /** L'utilisateur suit le seul sujet du catalogue, avec le parcours par défaut. */
    private static final class FollowingJava implements FollowedSubjects {
        @Override
        public List<String> of(UserRef user) {
            return List.of(SUBJECT);
        }

        @Override
        public Map<String, LearningTrack> tracksOf(UserRef user) {
            return Map.of();
        }
    }

    /** Les cartes introduites sont en révision, dues dans un an : seul le quota de nouvelles cartes est en jeu. */
    private static final class InMemoryStates implements ReviewStateRepository {

        private final Map<UUID, ReviewState> states = new HashMap<>();
        private final Map<UUID, Instant> introductions = new HashMap<>();

        void introduce(UUID cardId, Instant at) {
            states.put(
                    cardId, new ReviewState(LearningState.REVIEW, 0, 300, 5, at.plus(Duration.ofDays(365)), 1, 0, at));
            introductions.put(cardId, at);
        }

        @Override
        public Map<UUID, ReviewState> findAllByUser(UUID userId) {
            return Map.copyOf(states);
        }

        @Override
        public Optional<ReviewState> findForUpdate(UUID userId, UUID cardId) {
            return Optional.ofNullable(states.get(cardId));
        }

        @Override
        public void save(UUID userId, UUID cardId, ReviewState state) {
            states.put(cardId, state);
        }

        @Override
        public Set<UUID> cardsIntroducedSince(UUID userId, Instant since) {
            return introductions.entrySet().stream()
                    .filter(introduction -> !introduction.getValue().isBefore(since))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }
}
