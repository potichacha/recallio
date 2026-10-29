package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.RequestIdConflictException;
import com.recallio.review.domain.ReviewLogEntry;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.fsrs.FsrsScheduler;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : enregistrer la note donnée à une carte. Idempotent (CLAUDE.md, section 4.4) : sur mobile, une requête
 * peut être renvoyée après une coupure ; le même {@code requestId} renvoie alors le résultat déjà enregistré.
 */
@ApplicationScoped
public class ReviewRecordingService {

    private final CardDirectory cards;
    private final ReviewStateRepository states;
    private final ReviewLogRepository log;
    private final FsrsScheduler scheduler;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public ReviewRecordingService(
            CardDirectory cards,
            ReviewStateRepository states,
            ReviewLogRepository log,
            FsrsScheduler scheduler,
            IdGenerator idGenerator,
            Clock clock) {
        this.cards = cards;
        this.states = states;
        this.log = log;
        this.scheduler = scheduler;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * L'état de la carte est verrouillé avant de vérifier l'identifiant de requête : deux envois simultanés de la même
     * réponse sont sérialisés, et le second voit l'entrée du premier. Pour une carte nouvelle (rien à verrouiller),
     * l'unicité du {@code requestId} en base départage les deux envois.
     */
    @Transactional
    public ReviewOutcome record(UserRef user, RecordReview command) {
        CardSnapshot card =
                cards.activeCard(command.cardId()).orElseThrow(() -> new CardNotReviewableException(command.cardId()));
        Instant now = clock.instant();
        ReviewState current = states.findForUpdate(user.id(), card.cardId()).orElse(ReviewState.newCard(now));

        Optional<ReviewLogEntry> previous = log.findByRequestId(command.requestId());
        if (previous.isPresent()) {
            return replay(user, command, previous.get());
        }

        ReviewState next = scheduler.review(current, command.rating(), now);
        ReviewLogEntry entry = new ReviewLogEntry(
                idGenerator.newId(),
                command.requestId(),
                user.id(),
                card.cardId(),
                card.versionId(),
                command.rating(),
                now,
                command.elapsedMs(),
                next.state(),
                next.stability(),
                next.difficulty(),
                next.dueAt());
        if (!log.appendIfAbsent(entry)) {
            return replay(
                    user, command, log.findByRequestId(command.requestId()).orElseThrow());
        }
        states.save(user.id(), card.cardId(), next);
        return ReviewOutcome.of(entry, false);
    }

    private static ReviewOutcome replay(UserRef user, RecordReview command, ReviewLogEntry previous) {
        boolean sameReview = previous.userId().equals(user.id())
                && previous.cardId().equals(command.cardId())
                && previous.rating() == command.rating();
        if (!sameReview) {
            throw new RequestIdConflictException(command.requestId());
        }
        return ReviewOutcome.of(previous, true);
    }

    /**
     * @param requestId identifiant choisi par le client, unique par réponse
     * @param elapsedMs temps de réflexion, facultatif ; il ne rapporte rien (pas de prime à la vitesse)
     */
    public record RecordReview(
            UUID requestId,
            UUID cardId,
            Rating rating,
            @Nullable Integer elapsedMs) {}

    /** @param replayed {@code true} si la réponse avait déjà été enregistrée par une requête précédente */
    public record ReviewOutcome(UUID cardId, Rating rating, LearningState state, Instant dueAt, boolean replayed) {

        static ReviewOutcome of(ReviewLogEntry entry, boolean replayed) {
            return new ReviewOutcome(entry.cardId(), entry.rating(), entry.state(), entry.dueAt(), replayed);
        }
    }
}
