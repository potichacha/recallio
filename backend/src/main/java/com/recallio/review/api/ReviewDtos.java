package com.recallio.review.api;

import com.recallio.content.CardType;
import com.recallio.review.application.ReviewQueueService.QueuedCard;
import com.recallio.review.application.ReviewQueueService.RatingPreview;
import com.recallio.review.application.ReviewQueueService.ReviewQueue;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
            String themeSlug,
            String themeTitle,
            @Schema(description = "Recto, en Markdown") String prompt,
            @Schema(description = "Verso, en Markdown") String answer,
            LearningState state,

            @Schema(description = "Échéance obtenue pour chacune des quatre notes")
            List<RatingPreviewDto> previews) {
        static QueuedCardDto from(QueuedCard queued) {
            var card = queued.card();
            return new QueuedCardDto(
                    card.cardId(),
                    card.type(),
                    card.themeSlug(),
                    card.themeTitle(),
                    card.prompt(),
                    card.answer(),
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
            @NotNull Rating rating,

            @Schema(description = "Temps de réflexion en millisecondes", nullable = true)
            @PositiveOrZero
            @Max(MAX_ELAPSED_MS)
            @Nullable
            Integer elapsedMs) {}

    @Schema(name = "ReviewResult")
    record ReviewResultDto(UUID cardId, Rating rating, LearningState state, Instant dueAt) {
        static ReviewResultDto from(ReviewOutcome outcome) {
            return new ReviewResultDto(outcome.cardId(), outcome.rating(), outcome.state(), outcome.dueAt());
        }
    }
}
