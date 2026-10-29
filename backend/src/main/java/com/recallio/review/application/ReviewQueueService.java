package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.fsrs.FsrsScheduler;
import com.recallio.review.domain.queue.ReviewQueuePlanner;
import com.recallio.review.domain.queue.ReviewQueuePlanner.QueuePlan;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Cas d'usage : quelles cartes réviser maintenant (CLAUDE.md, section 6.1). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ReviewQueueService {

    private final CardDirectory cards;
    private final ReviewStateRepository states;
    private final FsrsScheduler scheduler;
    private final ReviewConfig config;
    private final Clock clock;

    public ReviewQueueService(
            CardDirectory cards,
            ReviewStateRepository states,
            FsrsScheduler scheduler,
            ReviewConfig config,
            Clock clock) {
        this.cards = cards;
        this.states = states;
        this.scheduler = scheduler;
        this.config = config;
        this.clock = clock;
    }

    public ReviewQueue queue(UserRef user, CardFilter filter, int size) {
        Instant now = clock.instant();
        Map<UUID, ReviewState> memory = states.findAllByUser(user.id());
        QueuePlan plan = ReviewQueuePlanner.plan(
                cards.activeCardIds(filter), memory, now, newCardsAllowedToday(user, now), size);
        List<QueuedCard> queued = cards.currentVersions(plan.cardIds()).stream()
                .map(card -> queuedCard(card, memory.getOrDefault(card.cardId(), ReviewState.newCard(now)), now))
                .toList();
        return new ReviewQueue(queued, plan.dueCount(), plan.newCount(), plan.nextDueAt());
    }

    /** Le quota de nouvelles cartes repart à zéro à minuit, dans le fuseau de l'utilisateur. */
    private int newCardsAllowedToday(UserRef user, Instant now) {
        Instant startOfDay = LocalDate.ofInstant(now, user.timezone())
                .atStartOfDay(user.timezone())
                .toInstant();
        long introducedToday = states.countIntroducedSince(user.id(), startOfDay);
        return (int) Math.max(0, config.newCardsPerDay() - introducedToday);
    }

    private QueuedCard queuedCard(CardSnapshot card, ReviewState state, Instant now) {
        List<RatingPreview> previews = Arrays.stream(Rating.values())
                .map(rating -> new RatingPreview(
                        rating, scheduler.review(state, rating, now).dueAt()))
                .toList();
        return new QueuedCard(card, state.state(), previews);
    }

    /**
     * @param dueCount cartes dues au total (la file peut en contenir moins)
     * @param newCount cartes nouvelles encore disponibles aujourd'hui
     * @param nextDueAt prochaine échéance, quand plus rien n'est dû
     */
    public record ReviewQueue(
            List<QueuedCard> cards,
            int dueCount,
            int newCount,
            @Nullable Instant nextDueAt) {}

    /** Une carte à réviser, avec l'échéance que donnerait chaque note (affichée sur les boutons). */
    public record QueuedCard(CardSnapshot card, LearningState state, List<RatingPreview> previews) {}

    public record RatingPreview(Rating rating, Instant dueAt) {}
}
