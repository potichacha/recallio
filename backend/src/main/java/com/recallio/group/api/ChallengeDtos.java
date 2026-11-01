package com.recallio.group.api;

import com.recallio.content.CardChoice;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.application.ChallengePlayService.AnswerResult;
import com.recallio.group.application.ChallengePlayService.PresentedCard;
import com.recallio.group.application.ChallengePlayService.Step;
import com.recallio.group.application.ChallengeService.ChallengeDetail;
import com.recallio.group.application.ChallengeService.ChallengeStatus;
import com.recallio.group.application.ChallengeService.ChallengeView;
import com.recallio.group.application.ChallengeService.Standing;
import com.recallio.group.domain.Challenge;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Contrat JSON des défis de groupe (ADR 0020). */
final class ChallengeDtos {

    private ChallengeDtos() {}

    @Schema(name = "CreateChallengeRequest")
    record CreateChallengeRequest(
            @NotBlank @Size(max = Challenge.TITLE_MAX_LENGTH)
            String title,

            @Min(Challenge.MIN_CARDS) @Max(Challenge.MAX_CARDS)
            int cardCount,

            @Min(Challenge.MIN_SECONDS) @Max(Challenge.MAX_SECONDS)
            int secondsPerCard) {}

    @Schema(name = "ChallengeSummary")
    record ChallengeSummaryDto(
            UUID id,
            String title,
            int cardCount,
            int secondsPerCard,
            Instant endsAt,

            @Schema(description = "Vrai tant qu'on peut y jouer")
            boolean open,

            ChallengeStatus myStatus,

            @Schema(description = "Le score de l'utilisateur, une fois sa partie terminée", nullable = true) @Nullable
            Integer myScore) {
        static ChallengeSummaryDto from(ChallengeView view) {
            Challenge challenge = view.challenge();
            return new ChallengeSummaryDto(
                    challenge.id(),
                    challenge.title(),
                    challenge.cardCount(),
                    (int) challenge.perCard().toSeconds(),
                    challenge.endsAt(),
                    view.open(),
                    view.myStatus(),
                    view.myScore());
        }
    }

    @Schema(name = "Challenge")
    record ChallengeDto(
            ChallengeSummaryDto summary,
            UUID groupId,
            String groupName,
            LeaderboardVisibility visibility,

            @Schema(
                    description =
                            "Les parties terminées, la meilleure d'abord ; seulement la sienne si le classement est"
                                    + " désactivé")
            List<ChallengeStandingDto> standings) {
        static ChallengeDto from(ChallengeDetail detail) {
            return new ChallengeDto(
                    ChallengeSummaryDto.from(detail.challenge()),
                    detail.challenge().challenge().groupId(),
                    detail.groupName(),
                    detail.visibility(),
                    detail.standings().stream().map(ChallengeStandingDto::from).toList());
        }
    }

    @Schema(name = "ChallengeStanding")
    record ChallengeStandingDto(
            int rank,

            @Schema(description = "Absent dans un classement anonymisé, sauf pour l'utilisateur", nullable = true)
            @Nullable
            String displayName,

            boolean me,
            int score,

            @Schema(description = "Temps passé à répondre, en millisecondes : il départage les ex æquo")
            long timeMs) {
        static ChallengeStandingDto from(Standing standing) {
            return new ChallengeStandingDto(
                    standing.rank(),
                    standing.displayName(),
                    standing.me(),
                    standing.score(),
                    standing.time().toMillis());
        }
    }

    @Schema(name = "ChallengeStep")
    record ChallengeStepDto(
            @Schema(description = "La carte à jouer ; absente une fois la partie terminée", nullable = true) @Nullable
            ChallengeCardDto card,

            int cardCount,

            @Schema(description = "Bonnes réponses jusqu'ici")
            int score,

            boolean finished) {
        static ChallengeStepDto from(Step step) {
            PresentedCard card = step.card();
            return new ChallengeStepDto(
                    card == null ? null : ChallengeCardDto.from(card), step.cardCount(), step.score(), step.finished());
        }
    }

    /** Comme dans la file de révision, un QCM arrive sans ses bonnes réponses ni son explication. */
    @Schema(name = "ChallengeCard")
    record ChallengeCardDto(
            @Schema(description = "Position dans la série, à partir de 0")
            int position,

            String subjectTitle,
            String themeTitle,
            @Schema(description = "En Markdown") String prompt,

            @Schema(description = "En Markdown, sans dire lesquels sont justes")
            List<String> choices,

            boolean multipleAnswers,

            @Schema(description = "Temps restant pour répondre, mesuré par le serveur")
            long remainingMs) {
        static ChallengeCardDto from(PresentedCard presented) {
            var card = presented.card();
            return new ChallengeCardDto(
                    presented.position(),
                    card.subjectTitle(),
                    card.themeTitle(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    presented.remaining().toMillis());
        }
    }

    @Schema(name = "ChallengeAnswerRequest")
    record ChallengeAnswerRequest(
            @PositiveOrZero int position,

            @Schema(description = "Positions des choix cochés, à partir de 0")
            @NotNull
            @Size(max = CardChoice.MAX_PER_CARD)
            List<@NotNull @PositiveOrZero Integer> selectedChoices) {}

    @Schema(name = "ChallengeAnswerResult")
    record ChallengeAnswerResultDto(
            boolean correct,

            @Schema(description = "La réponse est arrivée après le délai : elle compte comme fausse")
            boolean timedOut,

            List<Integer> correctChoices,
            @Schema(description = "En Markdown") String explanation,

            @Schema(description = "Vrai si c'était la dernière carte")
            boolean finished) {
        static ChallengeAnswerResultDto from(AnswerResult result) {
            return new ChallengeAnswerResultDto(
                    result.correct(),
                    result.timedOut(),
                    result.correctChoices().stream().sorted().toList(),
                    result.explanation(),
                    result.finished());
        }
    }
}
