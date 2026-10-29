package com.recallio.review.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardType;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.fsrs.FsrsParameters;
import com.recallio.review.domain.fsrs.FsrsScheduler;
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
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReviewQueueServiceTest {

    private static final int NEW_CARDS_PER_DAY = 2;
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final List<UUID> catalog =
            IntStream.range(0, 5).mapToObj(_ -> UUID.randomUUID()).toList();
    private final InMemoryStates states = new InMemoryStates();
    private final UserRef user = new UserRef(UUID.randomUUID(), PARIS);

    @Test
    void shouldCountCardsIntroducedSinceMidnightInTheUserTimezone() {
        // 23 h 30 à Paris : la carte vue à 00 h 10 (heure de Paris) compte dans le quota du jour.
        Instant now = Instant.parse("2026-10-29T22:30:00Z");
        states.introduce(catalog.get(0), Instant.parse("2026-10-28T23:10:00Z"));

        var queue = serviceAt(now).queue(user, CardDirectory.CardFilter.all(), 10);

        assertThat(queue.newCount()).isEqualTo(1);
    }

    @Test
    void shouldResetTheNewCardQuotaAtMidnightInTheUserTimezone() {
        // 00 h 30 à Paris le lendemain, mais encore la veille en UTC : le quota est renouvelé.
        states.introduce(catalog.get(0), Instant.parse("2026-10-29T10:00:00Z"));
        states.introduce(catalog.get(1), Instant.parse("2026-10-29T11:00:00Z"));
        Instant justAfterMidnightInParis = Instant.parse("2026-10-29T23:30:00Z");

        var queue = serviceAt(justAfterMidnightInParis).queue(user, CardDirectory.CardFilter.all(), 10);

        assertThat(queue.newCount()).isEqualTo(NEW_CARDS_PER_DAY);
    }

    @Test
    void shouldPreviewTheDueDateOfEachRating() {
        Instant now = Instant.parse("2026-10-29T08:00:00Z");

        var queue = serviceAt(now).queue(user, CardDirectory.CardFilter.all(), 1);

        var card = queue.cards().getFirst();
        assertThat(card.state()).isEqualTo(LearningState.NEW);
        assertThat(card.previews())
                .extracting(ReviewQueueService.RatingPreview::dueAt)
                .containsExactly(
                        now.plus(Duration.ofMinutes(1)),
                        now.plus(Duration.ofSeconds(330)),
                        now.plus(Duration.ofMinutes(10)),
                        now.plus(Duration.ofDays(8)));
    }

    private ReviewQueueService serviceAt(Instant now) {
        return new ReviewQueueService(
                new FixedCatalog(catalog),
                states,
                new FsrsScheduler(FsrsParameters.DEFAULT),
                new TestConfig(),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private record TestConfig() implements ReviewConfig {
        @Override
        public int newCardsPerDay() {
            return NEW_CARDS_PER_DAY;
        }

        @Override
        public double desiredRetention() {
            return FsrsParameters.DEFAULT.desiredRetention();
        }
    }

    private record FixedCatalog(List<UUID> ids) implements CardDirectory {
        @Override
        public List<UUID> activeCardIds(CardFilter filter) {
            return ids;
        }

        @Override
        public List<CardSnapshot> currentVersions(Collection<UUID> cardIds) {
            return cardIds.stream()
                    .map(id -> new CardSnapshot(
                            id, UUID.randomUUID(), CardType.FLASHCARD, "theme", "Thème", "Question ?", "Réponse."))
                    .toList();
        }
    }

    /** Les cartes introduites sont en révision, dues dans un an : seul le quota de nouvelles cartes est en jeu. */
    private static final class InMemoryStates implements ReviewStateRepository {

        private final Map<UUID, ReviewState> states = new HashMap<>();
        private final List<Instant> introductions = new ArrayList<>();

        void introduce(UUID cardId, Instant at) {
            states.put(
                    cardId, new ReviewState(LearningState.REVIEW, 0, 300, 5, at.plus(Duration.ofDays(365)), 1, 0, at));
            introductions.add(at);
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
        public long countIntroducedSince(UUID userId, Instant since) {
            return introductions.stream().filter(at -> !at.isBefore(since)).count();
        }
    }
}
