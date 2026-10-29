package com.recallio.review.domain.fsrs;

import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.ReviewState;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Planification par répétition espacée FSRS-6 (CLAUDE.md, section 6.2 ; ADR 0011). Pur et déterministe : le même état,
 * la même note et le même instant donnent toujours le même résultat (pas de « fuzz » aléatoire).
 *
 * <p>Une carte nouvelle passe par les étapes d'apprentissage (1 min, 10 min), puis par des intervalles en jours
 * calculés à partir de sa stabilité. Un oubli d'une carte mémorisée la renvoie en réapprentissage (10 min).
 */
public final class FsrsScheduler {

    private final FsrsParameters parameters;
    private final FsrsModel model;

    public FsrsScheduler(FsrsParameters parameters) {
        this.parameters = parameters;
        this.model = new FsrsModel(parameters);
    }

    public ReviewState review(ReviewState card, Rating rating, Instant now) {
        Memory memory = nextMemory(card, rating, now);
        Transition transition = switch (card.state()) {
            case NEW, LEARNING ->
                throughSteps(parameters.learningSteps(), LearningState.LEARNING, card.step(), rating, memory);
            case RELEARNING ->
                throughSteps(parameters.relearningSteps(), LearningState.RELEARNING, card.step(), rating, memory);
            case REVIEW -> fromReview(rating, memory);
        };
        boolean lapse = card.state() == LearningState.REVIEW && rating == Rating.AGAIN;
        return new ReviewState(
                transition.state(),
                transition.step(),
                memory.stability(),
                memory.difficulty(),
                now.plus(transition.interval()),
                card.reps() + 1,
                card.lapses() + (lapse ? 1 : 0),
                now);
    }

    private Memory nextMemory(ReviewState card, Rating rating, Instant now) {
        if (card.state() == LearningState.NEW) {
            return new Memory(model.initialStability(rating), model.initialDifficulty(rating));
        }
        Instant lastReview =
                Objects.requireNonNull(card.lastReviewAt(), "Une carte déjà révisée a une date de révision");
        long elapsedDays = Duration.between(lastReview, now).toDays();
        double stability = elapsedDays < 1
                ? model.shortTermStability(card.stability(), rating)
                : model.nextStability(
                        card.difficulty(),
                        card.stability(),
                        model.retrievability(elapsedDays, card.stability()),
                        rating);
        return new Memory(stability, model.nextDifficulty(card.difficulty(), rating));
    }

    /** Apprentissage et réapprentissage : on avance d'étape en étape, puis la carte passe en révision. */
    private Transition throughSteps(
            List<Duration> steps, LearningState stepState, int step, Rating rating, Memory memory) {
        if (steps.isEmpty() || (step >= steps.size() && rating != Rating.AGAIN)) {
            return graduate(memory);
        }
        return switch (rating) {
            case AGAIN -> new Transition(stepState, 0, steps.getFirst());
            case HARD -> new Transition(stepState, step, hardStepInterval(steps, step));
            case GOOD ->
                step + 1 == steps.size() ? graduate(memory) : new Transition(stepState, step + 1, steps.get(step + 1));
            case EASY -> graduate(memory);
        };
    }

    /** « Difficile » reste à la même étape, avec un délai entre celui de l'étape et celui de la suivante. */
    private static Duration hardStepInterval(List<Duration> steps, int step) {
        if (step > 0) {
            return steps.get(step);
        }
        if (steps.size() == 1) {
            return steps.getFirst().multipliedBy(3).dividedBy(2);
        }
        return steps.get(0).plus(steps.get(1)).dividedBy(2);
    }

    private Transition fromReview(Rating rating, Memory memory) {
        if (rating == Rating.AGAIN && !parameters.relearningSteps().isEmpty()) {
            return new Transition(
                    LearningState.RELEARNING, 0, parameters.relearningSteps().getFirst());
        }
        return graduate(memory);
    }

    private Transition graduate(Memory memory) {
        return new Transition(LearningState.REVIEW, 0, Duration.ofDays(model.intervalDays(memory.stability())));
    }

    private record Memory(double stability, double difficulty) {}

    private record Transition(LearningState state, int step, Duration interval) {}
}
