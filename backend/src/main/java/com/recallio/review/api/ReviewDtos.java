package com.recallio.review.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.review.application.ReviewQueueService.QueuedCard;
import com.recallio.review.application.ReviewQueueService.RatingPreview;
import com.recallio.review.application.ReviewQueueService.ReviewQueue;
import com.recallio.review.application.ReviewRecordingService.Correction;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Contrat JSON du module review : le domaine n'est jamais exposé directement. */
final class ReviewDtos {

    /** Au-delà d'une heure, le temps de réflexion n'a plus de sens (application laissée ouverte). */
    static final int MAX_ELAPSED_MS = 3_600_000;

    private ReviewDtos() {}

    @Schema(name = "ReviewQueue")
    record ReviewQueueDto(
            List<QueuedCardDto> cards,

            @Schema(description = "Cartes dues au total, la file peut en contenir moins")
            int dueCount,

            @Schema(description = "Cartes nouvelles encore disponibles aujourd'hui")
            int newCount,

            @Schema(description = "Prochaine échéance quand plus rien n'est dû", nullable = true) @Nullable
            Instant nextDueAt) {
        static ReviewQueueDto from(ReviewQueue queue) {
            return new ReviewQueueDto(
                    queue.cards().stream().map(QueuedCardDto::from).toList(),
                    queue.dueCount(),
                    queue.newCount(),
                    queue.nextDueAt());
        }
    }

    @Schema(name = "QueuedCard")
    record QueuedCardDto(
            UUID cardId,
            CardType type,
            String subjectSlug,
            String subjectTitle,
            String themeSlug,
            String themeTitle,
            @Schema(description = "Recto, en Markdown") String prompt,

            @Schema(
                    description = "Verso, en Markdown ; absent pour un QCM, dont l'explication arrive avec la"
                            + " correction",
                    nullable = true)
            @Nullable
            String answer,

            @Schema(description = "Les choix d'un QCM, en Markdown, sans dire lesquels sont justes ; vide sinon")
            List<String> choices,

            @Schema(
                    description =
                            "Vrai si le QCM a plusieurs bonnes réponses (cases à cocher plutôt que boutons radio)")
            boolean multipleAnswers,

            LearningState state,

            @Schema(description = "Échéance obtenue pour chacune des quatre notes")
            List<RatingPreviewDto> previews) {
        static QueuedCardDto from(QueuedCard queued) {
            var card = queued.card();
            boolean multipleChoice = card.type() == CardType.MULTIPLE_CHOICE;
            return new QueuedCardDto(
                    card.cardId(),
                    card.type(),
                    card.subjectSlug(),
                    card.subjectTitle(),
                    card.themeSlug(),
                    card.themeTitle(),
                    card.prompt(),
                    multipleChoice ? null : card.answer(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    queued.state(),
                    queued.previews().stream().map(RatingPreviewDto::from).toList());
        }
    }

    @Schema(name = "RatingPreview")
    record RatingPreviewDto(Rating rating, Instant dueAt) {
        static RatingPreviewDto from(RatingPreview preview) {
            return new RatingPreviewDto(preview.rating(), preview.dueAt());
        }
    }

    @Schema(name = "RecordReviewRequest")
    record RecordReviewRequest(
            @Schema(description = "Identifiant généré par le client, réutilisé tel quel si la requête est renvoyée")
            @NotNull
            UUID requestId,

            @NotNull UUID cardId,

            @Schema(
                    description = "La note d'auto-évaluation ; absente pour un QCM, que le serveur corrige",
                    nullable = true)
            @Nullable
            Rating rating,

            @Schema(
                    description = "Pour un QCM seulement : les positions des choix cochés, à partir de 0",
                    nullable = true)
            @Size(max = CardChoice.MAX_PER_CARD)
            @Nullable
            List<@NotNull @PositiveOrZero Integer> selectedChoices,

            @Schema(description = "Temps de réflexion en millisecondes", nullable = true)
            @PositiveOrZero
            @Max(MAX_ELAPSED_MS)
            @Nullable
            Integer elapsedMs) {}

    @Schema(name = "ReviewResult")
    record ReviewResultDto(
            UUID cardId,

            @Schema(description = "La note enregistrée ; pour un QCM, celle que donne la correction")
            Rating rating,

            LearningState state,
            Instant dueAt,

            @Schema(description = "La correction d'un QCM ; absente pour les autres formats", nullable = true) @Nullable
            CorrectionDto correction) {
        static ReviewResultDto from(ReviewOutcome outcome) {
            Correction correction = outcome.correction();
            return new ReviewResultDto(
                    outcome.cardId(),
                    outcome.rating(),
                    outcome.state(),
                    outcome.dueAt(),
                    correction == null ? null : CorrectionDto.from(correction));
        }
    }

    @Schema(name = "Correction")
    record CorrectionDto(
            @Schema(description = "Positions des bonnes réponses, à partir de 0, dans l'ordre")
            List<Integer> correctChoices,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {
        static CorrectionDto from(Correction correction) {
            return new CorrectionDto(
                    correction.correctChoices().stream().sorted().toList(), correction.explanation());
        }
    }
}
