package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.CardReviewed;
import com.recallio.review.domain.AnswerMismatchException;
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
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
    private final Event<CardReviewed> cardReviewed;

    public ReviewRecordingService(
            CardDirectory cards,
            ReviewStateRepository states,
            ReviewLogRepository log,
            FsrsScheduler scheduler,
            IdGenerator idGenerator,
            Clock clock,
            Event<CardReviewed> cardReviewed) {
        this.cards = cards;
        this.states = states;
        this.log = log;
        this.scheduler = scheduler;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.cardReviewed = cardReviewed;
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

        Rating rating = rating(card, command);
        Optional<ReviewLogEntry> previous = log.findByRequestId(command.requestId());
        if (previous.isPresent()) {
            return replay(user, command, rating, previous.get(), card);
        }

        ReviewState next = scheduler.review(current, rating, now);
        ReviewLogEntry entry = new ReviewLogEntry(
                idGenerator.newId(),
                command.requestId(),
                user.id(),
                card.cardId(),
                card.versionId(),
                rating,
                now,
                command.elapsedMs(),
                next.state(),
                next.stability(),
                next.difficulty(),
                next.dueAt());
        if (!log.appendIfAbsent(entry)) {
            return replay(
                    user,
                    command,
                    rating,
                    log.findByRequestId(command.requestId()).orElseThrow(),
                    card);
        }
        states.save(user.id(), card.cardId(), next);
        // Seulement pour une réponse nouvelle : une requête rejouée ne rapporte rien une seconde fois.
        cardReviewed.fire(new CardReviewed(user, entry.id(), card.cardId(), now));
        return ReviewOutcome.of(entry, false, correction(card));
    }

    /**
     * La note choisie par l'utilisateur, ou, pour un QCM ou un vrai ou faux, celle que donne la correction de ses choix
     * (ADR 0019, 0027).
     *
     * @throws AnswerMismatchException si la réponse ne correspond pas au format de la carte
     */
    private static Rating rating(CardSnapshot card, RecordReview command) {
        Set<Integer> selected = command.selectedChoices();
        Rating chosen = command.rating();
        if (card.type().isGradedByChoices()) {
            if (selected == null || chosen != null) {
                throw AnswerMismatchException.choicesExpected();
            }
            return Rating.ofGradedAnswer(card.isCorrectSelection(selected));
        }
        if (chosen == null || selected != null) {
            throw AnswerMismatchException.ratingExpected();
        }
        return chosen;
    }

    /** Pour un QCM : les bonnes réponses et l'explication, montrées une fois la réponse enregistrée. */
    private static @Nullable Correction correction(CardSnapshot card) {
        return card.type().isGradedByChoices() ? new Correction(card.correctChoices(), card.answer()) : null;
    }

    private static ReviewOutcome replay(
            UserRef user, RecordReview command, Rating rating, ReviewLogEntry previous, CardSnapshot card) {
        boolean sameReview = previous.userId().equals(user.id())
                && previous.cardId().equals(command.cardId())
                && previous.rating() == rating;
        if (!sameReview) {
            throw new RequestIdConflictException(command.requestId());
        }
        return ReviewOutcome.of(previous, true, correction(card));
    }

    /**
     * @param requestId identifiant choisi par le client, unique par réponse
     * @param rating la note d'auto-évaluation ; absente pour un QCM
     * @param selectedChoices les positions cochées d'un QCM (à partir de 0) ; absentes pour les autres formats
     * @param elapsedMs temps de réflexion, facultatif ; il ne rapporte rien (pas de prime à la vitesse)
     */
    public record RecordReview(
            UUID requestId,
            UUID cardId,
            @Nullable Rating rating,
            @Nullable Set<Integer> selectedChoices,
            @Nullable Integer elapsedMs) {

        public RecordReview {
            selectedChoices = selectedChoices == null ? null : Set.copyOf(selectedChoices);
        }
    }

    /**
     * @param replayed {@code true} si la réponse avait déjà été enregistrée par une requête précédente
     * @param correction la correction d'un QCM ; absente pour les autres formats
     */
    public record ReviewOutcome(
            UUID cardId,
            Rating rating,
            LearningState state,
            Instant dueAt,
            boolean replayed,
            @Nullable Correction correction) {

        static ReviewOutcome of(ReviewLogEntry entry, boolean replayed, @Nullable Correction correction) {
            return new ReviewOutcome(
                    entry.cardId(), entry.rating(), entry.state(), entry.dueAt(), replayed, correction);
        }
    }

    /** @param correctChoices les positions des bonnes réponses, à partir de 0 */
    public record Correction(Set<Integer> correctChoices, String explanation) {

        public Correction {
            correctChoices = Set.copyOf(correctChoices);
        }
    }
}
