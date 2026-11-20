package com.recallio.review.api;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.application.AnswerCorrection;
import com.recallio.review.application.DailyChallengeService;
import com.recallio.review.application.DailyChallengeService.AnswerOutcome;
import com.recallio.review.application.DailyChallengeService.ChallengeAnswer;
import com.recallio.review.application.DailyChallengeService.ChallengeSummary;
import com.recallio.review.application.DailyChallengeService.ChallengeView;
import com.recallio.review.domain.daily.DailyChallenge;
import com.recallio.review.domain.daily.DailyChallengeAttempt;
import com.recallio.review.domain.daily.DailyChallengeState;
import com.recallio.review.domain.daily.DailyChallengeStats;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le défi du jour (ADR 0067) : cinq questions par sujet, les mêmes pour tous, une tentative. Il faut un compte, seul
 * moyen de garantir une seule tentative ; chacun ne voit que ses réponses, et des chiffres du jour agrégés.
 */
@Path("/api/v1/me/daily-challenges")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Défi du jour", description = "Cinq questions par sujet et par jour, les mêmes pour tous (ADR 0067)")
@SecurityRequirement(name = "SecurityScheme")
public class DailyChallengesResource {

    private final DailyChallengeService challenges;
    private final CurrentUser currentUser;

    public DailyChallengesResource(DailyChallengeService challenges, CurrentUser currentUser) {
        this.challenges = challenges;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "listDailyChallenges",
            summary = "Le défi du jour de chaque sujet qui en a un, les sujets suivis d'abord ; n'en tire aucun")
    @APIResponse(responseCode = "200", description = "Où en est l'utilisateur dans chacun")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public List<DailyChallengeSummaryDto> listDailyChallenges() {
        return challenges.summaries(currentUser.get()).stream()
                .map(DailyChallengeSummaryDto::from)
                .toList();
    }

    @GET
    @Path("/{subject}")
    @Operation(
            operationId = "getDailyChallenge",
            summary = "Le défi du jour d'un sujet, tiré s'il ne l'est pas encore ; celui de la veille s'il est à finir")
    @APIResponse(responseCode = "200", description = "Le défi, ses questions sans leurs réponses, et ce qui est joué")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu ou non publié",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Pas assez de questions dans ce sujet pour un défi",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public DailyChallengeDto getDailyChallenge(
            @Parameter(description = "Slug du sujet") @PathParam("subject") String subject) {
        return DailyChallengeDto.from(challenges.challenge(currentUser.get(), subject));
    }

    @POST
    @Path("/{challengeId}/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "answerDailyChallenge",
            summary = "Répond à une question du défi, une fois pour toutes : la correction tout de suite (idempotent"
                    + " grâce à requestId)")
    @APIResponse(
            responseCode = "200",
            description = "La correction, où en est le défi, et les chiffres du jour s'il est fini")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Défi inconnu, ou sans question à cette position",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Question déjà répondue, défi fermé, ou requestId déjà utilisé pour une autre réponse",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "La réponse ne convient pas à la question",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public DailyChallengeAnswerResultDto answerDailyChallenge(
            @PathParam("challengeId") UUID challengeId, @Valid @NotNull DailyChallengeAnswerRequest request) {
        return DailyChallengeAnswerResultDto.from(
                challenges.answer(currentUser.get(), challengeId, request.toAnswer()));
    }

    @Schema(name = "DailyChallengeSummary")
    public record DailyChallengeSummaryDto(
            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Le jour du défi : aujourd'hui pour l'utilisateur, ou la veille s'il est à finir")
            LocalDate day,

            DailyChallengeState state,

            @Schema(description = "Les réponses justes, une fois le défi fini ; absent avant", nullable = true)
            @Nullable
            Integer score,

            @Schema(description = "Le nombre de questions du défi")
            int questionCount) {

        static DailyChallengeSummaryDto from(ChallengeSummary summary) {
            DailyChallengeAttempt attempt = summary.attempt();
            return attempt == null
                    ? new DailyChallengeSummaryDto(
                            summary.subjectSlug(),
                            summary.subjectTitle(),
                            summary.day(),
                            DailyChallengeState.NOT_STARTED,
                            null,
                            DailyChallenge.QUESTIONS)
                    : new DailyChallengeSummaryDto(
                            summary.subjectSlug(),
                            summary.subjectTitle(),
                            summary.day(),
                            attempt.state(),
                            scoreOf(attempt),
                            attempt.total());
        }
    }

    @Schema(name = "DailyChallenge")
    public record DailyChallengeDto(
            UUID id,
            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Le jour du défi : aujourd'hui pour l'utilisateur, ou la veille s'il est à finir")
            LocalDate day,

            DailyChallengeState state,

            @Schema(description = "Les réponses justes, une fois le défi fini ; absent avant", nullable = true)
            @Nullable
            Integer score,

            @Schema(description = "Le nombre de questions du défi")
            int questionCount,

            @Schema(description = "Les questions, de la plus facile à la plus difficile, sans leurs réponses")
            List<DailyChallengeQuestionDto> questions,

            @Schema(description = "Les réponses déjà données, avec leur correction, dans l'ordre des questions")
            List<DailyChallengeAnsweredDto> answers,

            @Schema(
                    description = "Les chiffres du jour, agrégés, une fois le défi fini ; absents avant, pour ne pas"
                            + " l'influencer",
                    nullable = true)
            @Nullable
            DailyChallengeStatsDto stats) {

        static DailyChallengeDto from(ChallengeView view) {
            DailyChallenge challenge = view.challenge();
            DailyChallengeAttempt attempt = view.attempt();
            return new DailyChallengeDto(
                    challenge.id(),
                    challenge.subjectSlug(),
                    view.subjectTitle(),
                    challenge.day(),
                    attempt.state(),
                    scoreOf(attempt),
                    attempt.total(),
                    view.questions().entrySet().stream()
                            .map(entry -> DailyChallengeQuestionDto.from(entry.getKey(), entry.getValue()))
                            .toList(),
                    view.questions().entrySet().stream()
                            .filter(entry -> attempt.isAnswered(entry.getKey()))
                            .map(entry -> DailyChallengeAnsweredDto.from(
                                    entry.getKey(), attempt.answers().get(entry.getKey()), entry.getValue()))
                            .toList(),
                    DailyChallengeStatsDto.from(view.stats()));
        }
    }

    @Schema(name = "DailyChallengeQuestion")
    public record DailyChallengeQuestionDto(
            @Schema(description = "La position de la question dans le défi, à partir de 0")
            int position,

            UUID cardId,
            CardType type,

            @Schema(description = "En Markdown ; les trous d'un code à compléter sont des ___")
            String prompt,

            @Schema(description = "Les choix d'une carte à choix, sans dire lesquels sont justes")
            List<String> choices,

            @Schema(description = "Vrai si plusieurs choix sont justes")
            boolean multipleAnswers,

            @Schema(description = "Le nombre de trous d'un code à compléter ; 0 sinon")
            int blankCount,

            @Schema(
                    description = "Le slug de la notion, qui mène à sa leçon ; absent hors d'un dossier de sujet",
                    nullable = true)
            @Nullable
            String notionSlug,

            @Schema(description = "Le titre de cette notion", nullable = true) @Nullable
            String notionTitle) {

        static DailyChallengeQuestionDto from(int position, CardSnapshot card) {
            return new DailyChallengeQuestionDto(
                    position,
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size(),
                    card.notionSlug(),
                    card.notionTitle());
        }
    }

    /** Une question déjà répondue : seul le verdict de la réponse est gardé, pas la réponse elle-même. */
    @Schema(name = "DailyChallengeAnswered")
    public record DailyChallengeAnsweredDto(
            int position,

            @Schema(description = "Vrai si la réponse était juste")
            boolean correct,

            @Schema(description = "Les bonnes réponses d'une carte à choix, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "Les réponses attendues d'un code à compléter, une par trou ; vide sinon")
            List<String> expectedBlanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static DailyChallengeAnsweredDto from(int position, @Nullable Boolean correct, CardSnapshot card) {
            return new DailyChallengeAnsweredDto(
                    position,
                    Boolean.TRUE.equals(correct),
                    card.correctChoices().stream().sorted().toList(),
                    card.blanks().stream().map(CardBlank::expected).toList(),
                    card.answer());
        }
    }

    @Schema(name = "DailyChallengeStats")
    public record DailyChallengeStatsDto(
            @Schema(description = "Les joueurs qui ont fini ce défi")
            int players,

            @Schema(description = "Leur score moyen, à une décimale")
            double averageScore,

            @Schema(description = "Le nombre de joueurs de chaque score, de 0 au nombre de questions")
            List<DailyChallengeScoreCountDto> distribution) {

        static @Nullable DailyChallengeStatsDto from(@Nullable DailyChallengeStats stats) {
            if (stats == null) {
                return null;
            }
            List<Integer> players = stats.playersByScore();
            return new DailyChallengeStatsDto(
                    stats.players(),
                    stats.averageScore(),
                    IntStream.range(0, players.size())
                            .mapToObj(score -> new DailyChallengeScoreCountDto(score, players.get(score)))
                            .toList());
        }
    }

    @Schema(name = "DailyChallengeScoreCount")
    public record DailyChallengeScoreCountDto(int score, int players) {}

    @Schema(name = "DailyChallengeAnswerRequest")
    public record DailyChallengeAnswerRequest(
            @Schema(description = "Identifiant généré par le client, réutilisé tel quel si la requête est renvoyée")
            @NotNull
            UUID requestId,

            @Schema(description = "La position de la question, à partir de 0")
            @NotNull
            @PositiveOrZero
            @Max(DailyChallenge.QUESTIONS - 1)
            Integer position,

            @NotNull @Valid DailyChallengeResponseDto answer) {

        ChallengeAnswer toAnswer() {
            List<Integer> selected = answer.selectedChoices();
            return new ChallengeAnswer(
                    requestId, position, selected == null ? null : Set.copyOf(selected), answer.filledBlanks());
        }
    }

    /** La réponse elle-même, au format de l'entraînement : une seule de ses deux formes. */
    @Schema(name = "DailyChallengeResponse")
    public record DailyChallengeResponseDto(
            @Schema(
                    description = "Pour une carte à choix : les positions cochées, à partir de 0, dans l'ordre de la"
                            + " carte (avant le mélange de l'affichage)",
                    nullable = true)
            @Size(max = CardChoice.MAX_PER_CARD)
            @Nullable
            List<@NotNull @PositiveOrZero Integer> selectedChoices,

            @Schema(description = "Pour un code à trous : la réponse de chaque trou, dans l'ordre", nullable = true)
            @Size(max = CardBlank.MAX_PER_CARD)
            @Nullable
            List<@NotNull @Size(max = ReviewDtos.MAX_BLANK_LENGTH) String> filledBlanks) {}

    @Schema(name = "DailyChallengeAnswerResult")
    public record DailyChallengeAnswerResultDto(
            DailyChallengeCorrectionDto correction,
            DailyChallengeState state,

            @Schema(description = "Les réponses justes, une fois le défi fini ; absent avant", nullable = true)
            @Nullable
            Integer score,

            @Schema(description = "Le nombre de questions du défi")
            int questionCount,

            @Schema(
                    description = "Les chiffres du jour, si cette réponse finit le défi ; absents sinon",
                    nullable = true)
            @Nullable
            DailyChallengeStatsDto stats) {

        static DailyChallengeAnswerResultDto from(AnswerOutcome outcome) {
            DailyChallengeAttempt attempt = outcome.attempt();
            return new DailyChallengeAnswerResultDto(
                    DailyChallengeCorrectionDto.from(outcome.position(), outcome.correction()),
                    attempt.state(),
                    scoreOf(attempt),
                    attempt.total(),
                    DailyChallengeStatsDto.from(outcome.stats()));
        }
    }

    @Schema(name = "DailyChallengeCorrection")
    public record DailyChallengeCorrectionDto(
            int position,
            boolean correct,

            @Schema(description = "Les bonnes réponses d'une carte à choix, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à compléter ; vide sinon")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static DailyChallengeCorrectionDto from(int position, AnswerCorrection correction) {
            return new DailyChallengeCorrectionDto(
                    position,
                    correction.correct(),
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation());
        }
    }

    private static @Nullable Integer scoreOf(DailyChallengeAttempt attempt) {
        return attempt.isDone() ? attempt.score() : null;
    }
}
