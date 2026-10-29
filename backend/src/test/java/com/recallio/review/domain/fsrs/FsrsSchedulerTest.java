package com.recallio.review.domain.fsrs;

import static com.recallio.review.domain.Rating.AGAIN;
import static com.recallio.review.domain.Rating.EASY;
import static com.recallio.review.domain.Rating.GOOD;
import static com.recallio.review.domain.Rating.HARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.ReviewState;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les scénarios « de référence » reprennent les tests de py-fsrs, l'implémentation officielle de FSRS-6
 * (tests/test_basic.py) : mêmes paramètres par défaut, sans fuzz, donc mêmes intervalles attendus.
 */
class FsrsSchedulerTest {

    private static final Instant START = Instant.parse("2022-11-29T12:30:00Z");

    private final FsrsScheduler scheduler = new FsrsScheduler(FsrsParameters.DEFAULT);

    @Nested
    class ReferenceScenarios {

        @Test
        void shouldProduceTheReferenceIntervalsWhenReviewingAtEachDueDate() {
            List<Rating> ratings =
                    List.of(GOOD, GOOD, GOOD, GOOD, GOOD, GOOD, AGAIN, AGAIN, GOOD, GOOD, GOOD, GOOD, GOOD);

            List<Long> intervals = new ArrayList<>();
            ReviewState card = ReviewState.newCard(START);
            Instant now = START;
            for (Rating rating : ratings) {
                card = scheduler.review(card, rating, now);
                intervals.add(Duration.between(now, card.dueAt()).toDays());
                now = card.dueAt();
            }

            assertThat(intervals).containsExactly(0L, 2L, 11L, 46L, 163L, 498L, 0L, 0L, 2L, 4L, 7L, 12L, 21L);
        }

        @Test
        void shouldMatchTheReferenceMemoryStateAfterAForgottenThenRecalledCard() {
            List<Rating> ratings = List.of(AGAIN, GOOD, GOOD, GOOD, GOOD, GOOD);
            List<Integer> daysBeforeEachReview = List.of(0, 0, 1, 3, 8, 21);

            ReviewState card = ReviewState.newCard(START);
            Instant now = START;
            for (int i = 0; i < ratings.size(); i++) {
                now = now.plus(Duration.ofDays(daysBeforeEachReview.get(i)));
                card = scheduler.review(card, ratings.get(i), now);
            }

            assertThat(card.stability()).isCloseTo(53.62691, within(1e-4));
            assertThat(card.difficulty()).isCloseTo(6.3574867, within(1e-4));
        }

        @Test
        void shouldReachTheMinimumDifficultyAfterRepeatedEasyReviews() {
            ReviewState card = ReviewState.newCard(START);
            Instant now = START;
            for (int i = 0; i < 10; i++) {
                card = scheduler.review(card, EASY, now);
                now = card.dueAt();
            }

            assertThat(card.difficulty()).isEqualTo(FsrsModel.MIN_DIFFICULTY);
        }
    }

    @Nested
    class LearningSteps {

        @Test
        void shouldShowANewCardAgainAfterOneMinuteWhenForgotten() {
            ReviewState card = scheduler.review(ReviewState.newCard(START), AGAIN, START);

            assertThat(card.state()).isEqualTo(LearningState.LEARNING);
            assertThat(card.step()).isZero();
            assertThat(card.dueAt()).isEqualTo(START.plus(Duration.ofMinutes(1)));
        }

        @Test
        void shouldWaitBetweenTheFirstTwoStepsWhenANewCardIsHard() {
            ReviewState card = scheduler.review(ReviewState.newCard(START), HARD, START);

            assertThat(card.state()).isEqualTo(LearningState.LEARNING);
            assertThat(card.step()).isZero();
            assertThat(card.dueAt()).isEqualTo(START.plus(Duration.ofSeconds(330)));
        }

        @Test
        void shouldGraduateAfterTheLastLearningStep() {
            ReviewState first = scheduler.review(ReviewState.newCard(START), GOOD, START);
            assertThat(first.state()).isEqualTo(LearningState.LEARNING);
            assertThat(first.step()).isEqualTo(1);
            assertThat(first.dueAt()).isEqualTo(START.plus(Duration.ofMinutes(10)));

            ReviewState second = scheduler.review(first, GOOD, first.dueAt());

            assertThat(second.state()).isEqualTo(LearningState.REVIEW);
            assertThat(Duration.between(first.dueAt(), second.dueAt())).isGreaterThanOrEqualTo(Duration.ofDays(1));
        }

        @Test
        void shouldGraduateImmediatelyWhenANewCardIsEasy() {
            ReviewState card = scheduler.review(ReviewState.newCard(START), EASY, START);

            assertThat(card.state()).isEqualTo(LearningState.REVIEW);
            assertThat(Duration.between(START, card.dueAt())).isGreaterThanOrEqualTo(Duration.ofDays(1));
        }

        @Test
        void shouldUseOneAndAHalfStepWhenHardWithASingleStep() {
            var oneStep = new FsrsScheduler(
                    FsrsParameters.DEFAULT.withSteps(List.of(Duration.ofMinutes(10)), List.of(Duration.ofMinutes(10))));

            ReviewState card = oneStep.review(ReviewState.newCard(START), HARD, START);

            assertThat(card.dueAt()).isEqualTo(START.plus(Duration.ofMinutes(15)));
        }

        @Test
        void shouldGoStraightToReviewWithoutLearningSteps() {
            var noSteps = new FsrsScheduler(FsrsParameters.DEFAULT.withSteps(List.of(), List.of()));

            ReviewState card = noSteps.review(ReviewState.newCard(START), AGAIN, START);

            assertThat(card.state()).isEqualTo(LearningState.REVIEW);
            assertThat(Duration.between(START, card.dueAt())).isGreaterThanOrEqualTo(Duration.ofDays(1));
        }
    }

    @Nested
    class Lapses {

        @Test
        void shouldSendAForgottenReviewCardToRelearningAndCountALapse() {
            ReviewState memorized = memorizedCard();

            ReviewState forgotten = scheduler.review(memorized, AGAIN, memorized.dueAt());

            assertThat(forgotten.state()).isEqualTo(LearningState.RELEARNING);
            assertThat(forgotten.dueAt()).isEqualTo(memorized.dueAt().plus(Duration.ofMinutes(10)));
            assertThat(forgotten.lapses()).isEqualTo(1);
            assertThat(forgotten.stability()).isLessThan(memorized.stability());
        }

        @Test
        void shouldReturnToReviewAfterRelearning() {
            ReviewState memorized = memorizedCard();
            ReviewState forgotten = scheduler.review(memorized, AGAIN, memorized.dueAt());

            ReviewState relearned = scheduler.review(forgotten, GOOD, forgotten.dueAt());

            assertThat(relearned.state()).isEqualTo(LearningState.REVIEW);
            assertThat(relearned.lapses()).isEqualTo(1);
            assertThat(relearned.reps()).isEqualTo(memorized.reps() + 2);
        }

        @Test
        void shouldNotCountALapseWhenALearningCardIsForgotten() {
            ReviewState learning = scheduler.review(ReviewState.newCard(START), GOOD, START);

            ReviewState forgotten = scheduler.review(learning, AGAIN, learning.dueAt());

            assertThat(forgotten.lapses()).isZero();
        }

        private ReviewState memorizedCard() {
            ReviewState card = ReviewState.newCard(START);
            Instant now = START;
            for (int i = 0; i < 3; i++) {
                card = scheduler.review(card, GOOD, now);
                now = card.dueAt();
            }
            assertThat(card.state()).isEqualTo(LearningState.REVIEW);
            return card;
        }
    }

    @Nested
    class Intervals {

        @Test
        void shouldScheduleLongerIntervalsForBetterRatings() {
            ReviewState learning = scheduler.review(ReviewState.newCard(START), GOOD, START);
            ReviewState memorized = scheduler.review(learning, GOOD, learning.dueAt());
            Instant reviewTime = memorized.dueAt();

            ReviewState hard = scheduler.review(memorized, HARD, reviewTime);
            ReviewState good = scheduler.review(memorized, GOOD, reviewTime);
            ReviewState easy = scheduler.review(memorized, EASY, reviewTime);

            assertThat(hard.dueAt()).isBefore(good.dueAt());
            assertThat(good.dueAt()).isBefore(easy.dueAt());
            assertThat(scheduler.review(memorized, AGAIN, reviewTime).state()).isEqualTo(LearningState.RELEARNING);
        }

        @Test
        void shouldScheduleShorterIntervalsForAHigherDesiredRetention() {
            var demanding = new FsrsScheduler(FsrsParameters.DEFAULT.withDesiredRetention(0.95));

            ReviewState standard = scheduler.review(ReviewState.newCard(START), EASY, START);
            ReviewState strict = demanding.review(ReviewState.newCard(START), EASY, START);

            assertThat(strict.dueAt()).isBefore(standard.dueAt());
        }

        @Test
        void shouldNeverExceedTheMaximumInterval() {
            var capped = new FsrsScheduler(new FsrsParameters(
                    FsrsParameters.DEFAULT.weights(),
                    0.9,
                    FsrsParameters.DEFAULT.learningSteps(),
                    FsrsParameters.DEFAULT.relearningSteps(),
                    30));
            ReviewState card = ReviewState.newCard(START);
            Instant now = START;
            for (int i = 0; i < 8; i++) {
                ReviewState next = capped.review(card, EASY, now);
                assertThat(Duration.between(now, next.dueAt())).isLessThanOrEqualTo(Duration.ofDays(30));
                card = next;
                now = next.dueAt();
            }
        }
    }
}
