package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.application.ReviewRecordingService.RecordReview;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : rendre un exercice de code (ADR 0030). Ses tests le jugent, puis la révision est enregistrée avec la
 * note qui en découle.
 *
 * <p>Le code s'exécute <em>avant</em> la transaction d'enregistrement, qui verrouille l'état de la carte : une exécution
 * dure quelques secondes. Une requête renvoyée (même {@code requestId}) exécute le code de nouveau ; le résultat étant
 * le même, l'enregistrement la reconnaît comme déjà reçue.
 */
@ApplicationScoped
public class ExerciseSubmissionService {

    private final CardDirectory cards;
    private final ExerciseRunService runs;
    private final ReviewRecordingService recordings;

    public ExerciseSubmissionService(CardDirectory cards, ExerciseRunService runs, ReviewRecordingService recordings) {
        this.cards = cards;
        this.runs = runs;
        this.recordings = recordings;
    }

    public ReviewOutcome submit(
            UserRef user, UUID requestId, UUID cardId, List<SourceFile> files, @Nullable Integer elapsedMs) {
        CardSnapshot card = cards.activeCard(cardId).orElseThrow(() -> new CardNotReviewableException(cardId));
        ExerciseRun run = runs.run(user, card, files);
        return recordings.record(user, RecordReview.exercise(requestId, cardId, run, elapsedMs));
    }
}
