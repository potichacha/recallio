package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
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
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.fsrs.FsrsScheduler;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
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
        @Nullable Correction correction = correction(card, command);
        Optional<ReviewLogEntry> previous = log.findByRequestId(command.requestId());
        if (previous.isPresent()) {
            return replay(user, command, rating, previous.get(), correction);
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
                    correction);
        }
        states.save(user.id(), card.cardId(), next);
        // Seulement pour une réponse nouvelle : une requête rejouée ne rapporte rien une seconde fois.
        cardReviewed.fire(new CardReviewed(user, entry.id(), card.cardId(), now));
        return ReviewOutcome.of(entry, false, correction);
    }

    /**
     * La note choisie par l'utilisateur, ou, pour un format corrigé par le serveur, celle que donne la correction de sa
     * réponse (ADR 0019, 0027) ou, pour un exercice de code, ses tests (ADR 0030).
     *
     * @throws AnswerMismatchException si la réponse ne correspond pas au format de la carte
     */
    private static Rating rating(CardSnapshot card, RecordReview command) {
        Set<Integer> selected = command.selectedChoices();
        List<String> filled = command.filledBlanks();
        ExerciseRun run = command.exerciseRun();
        Rating chosen = command.rating();
        if (card.type().isGradedByChoices()) {
            if (selected == null || chosen != null || filled != null || run != null) {
                throw AnswerMismatchException.choicesExpected();
            }
            return Rating.ofGradedAnswer(card.isCorrectSelection(selected));
        }
        if (card.type() == CardType.FILL_IN) {
            if (filled == null
                    || filled.size() != card.blanks().size()
                    || chosen != null
                    || selected != null
                    || run != null) {
                throw AnswerMismatchException.blanksExpected(card.blanks().size());
            }
            return Rating.ofGradedAnswer(!card.checkBlanks(filled).contains(false));
        }
        if (card.type() == CardType.CODING_EXERCISE) {
            if (run == null || chosen != null || selected != null || filled != null) {
                throw AnswerMismatchException.filesExpected();
            }
            return Rating.ofGradedAnswer(run.solved());
        }
        if (chosen == null || selected != null || filled != null || run != null) {
            throw AnswerMismatchException.ratingExpected();
        }
        return chosen;
    }

    /**
     * Pour un format corrigé par le serveur : les bonnes réponses et l'explication, montrées une fois la réponse
     * enregistrée. À appeler après {@link #rating}, qui vérifie la forme de la réponse.
     */
    private static @Nullable Correction correction(CardSnapshot card, RecordReview command) {
        if (card.type().isGradedByChoices()) {
            return new Correction(card.correctChoices(), List.of(), card.answer(), null, List.of());
        }
        ExerciseRun run = command.exerciseRun();
        ExerciseFiles exercise = card.exercise();
        if (card.type() == CardType.CODING_EXERCISE && run != null && exercise != null) {
            return new Correction(Set.of(), List.of(), card.answer(), run, exercise.solution());
        }
        List<String> filled = command.filledBlanks();
        if (card.type() == CardType.FILL_IN && filled != null) {
            List<Boolean> results = card.checkBlanks(filled);
            List<BlankCorrection> blanks = IntStream.range(0, results.size())
                    .mapToObj(index ->
                            new BlankCorrection(card.blanks().get(index).expected(), results.get(index)))
                    .toList();
            return new Correction(Set.of(), blanks, card.answer(), null, List.of());
        }
        return null;
    }

    private static ReviewOutcome replay(
            UserRef user,
            RecordReview command,
            Rating rating,
            ReviewLogEntry previous,
            @Nullable Correction correction) {
        boolean sameReview = previous.userId().equals(user.id())
                && previous.cardId().equals(command.cardId())
                && previous.rating() == rating;
        if (!sameReview) {
            throw new RequestIdConflictException(command.requestId());
        }
        return ReviewOutcome.of(previous, true, correction);
    }

    /**
     * @param requestId identifiant choisi par le client, unique par réponse
     * @param rating la note d'auto-évaluation ; absente pour un QCM
     * @param selectedChoices les positions cochées d'un QCM (à partir de 0) ; absentes pour les autres formats
     * @param filledBlanks les réponses d'un code à trous, une par trou ; absentes pour les autres formats
     * @param exerciseRun le résultat des tests d'un exercice de code rendu ({@link ExerciseSubmissionService}) ; absent
     *     pour les autres formats
     * @param elapsedMs temps de réflexion, facultatif ; il ne rapporte rien (pas de prime à la vitesse)
     */
    public record RecordReview(
            UUID requestId,
            UUID cardId,
            @Nullable Rating rating,
            @Nullable Set<Integer> selectedChoices,
            @Nullable List<String> filledBlanks,
            @Nullable ExerciseRun exerciseRun,
            @Nullable Integer elapsedMs) {

        public RecordReview {
            selectedChoices = selectedChoices == null ? null : Set.copyOf(selectedChoices);
            filledBlanks = filledBlanks == null ? null : List.copyOf(filledBlanks);
        }

        /** Un exercice rendu, jugé par ses tests. */
        static RecordReview exercise(UUID requestId, UUID cardId, ExerciseRun run, @Nullable Integer elapsedMs) {
            return new RecordReview(requestId, cardId, null, null, null, run, elapsedMs);
        }
    }

    /**
     * @param replayed {@code true} si la réponse avait déjà été enregistrée par une requête précédente
     * @param correction la correction d'un format corrigé par le serveur ; absente pour les autres formats
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

    /**
     * @param correctChoices les positions des bonnes réponses d'un QCM ou d'un vrai ou faux, à partir de 0
     * @param blanks la correction de chaque trou d'un code à trous, dans l'ordre
     * @param exerciseRun le résultat des tests d'un exercice de code ; absent pour les autres formats
     * @param solution la solution modèle d'un exercice de code, montrée une fois rendu ; vide pour les autres formats
     */
    public record Correction(
            Set<Integer> correctChoices,
            List<BlankCorrection> blanks,
            String explanation,
            @Nullable ExerciseRun exerciseRun,
            List<SourceFile> solution) {

        public Correction {
            correctChoices = Set.copyOf(correctChoices);
            blanks = List.copyOf(blanks);
            solution = List.copyOf(solution);
        }
    }

    /**
     * @param expected la réponse attendue (la première des réponses acceptées)
     * @param correct si la réponse donnée pour ce trou est acceptée
     */
    public record BlankCorrection(String expected, boolean correct) {}
}
