package com.recallio.review.api;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.application.AnswerCorrection;
import com.recallio.review.application.GradedAnswer;
import com.recallio.review.application.LevelCheckService;
import com.recallio.review.application.LevelCheckService.DomainOutcome;
import com.recallio.review.application.LevelCheckService.DomainQuestions;
import com.recallio.review.application.LevelCheckService.LevelCheck;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le test de niveau (ADR 0068) : deux questions par domaine des bases d'un sujet, pour sauter ce qu'on sait déjà dans le
 * Tour et en révision. Une révision personnelle, sans session gardée : chaque domaine se corrige seul. Chacun ne voit et
 * ne change que ses notions connues.
 */
@Path("/api/v1/me/level-checks/{subject}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Test de niveau", description = "Deux questions par domaine des bases d'un sujet (ADR 0068)")
@SecurityRequirement(name = "SecurityScheme")
public class LevelChecksResource {

    /** Bien au-delà des questions d'un domaine : une borne contre les requêtes démesurées, pas une règle du test. */
    static final int MAX_ANSWERS = 10;

    private final LevelCheckService levelChecks;
    private final CurrentUser currentUser;

    public LevelChecksResource(LevelCheckService levelChecks, CurrentUser currentUser) {
        this.levelChecks = levelChecks;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getLevelCheck",
            summary = "Le test de niveau d'un sujet, tiré à chaque appel : ses domaines des bases et leurs questions")
    @APIResponse(responseCode = "200", description = "Les domaines, dans l'ordre du référentiel, sans les réponses")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu ou non publié",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Le sujet n'a pas de notion débutante avec des questions",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LevelCheckDto getLevelCheck(@Parameter(description = "Slug du sujet") @PathParam("subject") String subject) {
        return LevelCheckDto.from(levelChecks.levelCheck(currentUser.get(), subject));
    }

    @POST
    @Path("/domains/{domain}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkLevelCheckDomain",
            summary =
                    "Corrige les réponses aux questions d'un domaine ; toutes justes, ses notions débutantes deviennent"
                            + " connues (idempotent : refaire n'enlève rien)")
    @APIResponse(responseCode = "200", description = "La correction, et les notions connues du sujet après elle")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu ou non publié, ou domaine absent de son test",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Les cartes ne sont pas celles d'un tirage du domaine, ou une réponse ne convient pas à sa"
                    + " question",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LevelCheckDomainResultDto checkLevelCheckDomain(
            @PathParam("subject") String subject,
            @Parameter(description = "L'identifiant du domaine dans le référentiel") @PathParam("domain") String domain,
            @Valid @NotNull LevelCheckAnswersRequest request) {
        List<GradedAnswer> answers =
                request.answers().stream().map(LevelCheckAnswerDto::toAnswer).toList();
        return LevelCheckDomainResultDto.from(levelChecks.check(currentUser.get(), subject, domain, answers));
    }

    @DELETE
    @Operation(
            operationId = "cancelLevelCheck",
            summary = "Annule le test de niveau d'un sujet : ses notions connues sont oubliées")
    @APIResponse(responseCode = "204", description = "Plus aucune notion connue dans ce sujet")
    public void cancelLevelCheck(@PathParam("subject") String subject) {
        levelChecks.cancel(currentUser.get(), subject);
    }

    @Schema(name = "LevelCheck")
    public record LevelCheckDto(
            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Les notions du sujet que l'utilisateur connaît déjà, d'un test précédent")
            int knownNotions,

            @Schema(description = "Les domaines des bases, dans l'ordre du référentiel")
            List<LevelCheckDomainDto> domains) {

        static LevelCheckDto from(LevelCheck check) {
            return new LevelCheckDto(
                    check.subjectSlug(),
                    check.subjectTitle(),
                    check.knownNotions(),
                    check.domains().stream().map(LevelCheckDomainDto::from).toList());
        }
    }

    @Schema(name = "LevelCheckDomain")
    public record LevelCheckDomainDto(
            @Schema(description = "L'identifiant du domaine dans le référentiel")
            String id,

            String title,

            @Schema(description = "Ses notions débutantes : celles que le réussir rend connues")
            int beginnerNotions,

            @Schema(description = "Vrai si toutes ses notions débutantes sont déjà connues")
            boolean known,

            @Schema(description = "Ses questions, une ou deux, de notions différentes, sans leurs réponses")
            List<LevelCheckQuestionDto> questions) {

        static LevelCheckDomainDto from(DomainQuestions domain) {
            return new LevelCheckDomainDto(
                    domain.domain().id(),
                    domain.domain().title(),
                    domain.domain().beginnerNotions().size(),
                    domain.known(),
                    domain.questions().stream().map(LevelCheckQuestionDto::from).toList());
        }
    }

    @Schema(name = "LevelCheckQuestion")
    public record LevelCheckQuestionDto(
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

            @Schema(description = "Le slug de la notion, qui mène à sa leçon", nullable = true) @Nullable
            String notionSlug,

            @Schema(description = "Le titre de cette notion", nullable = true) @Nullable
            String notionTitle) {

        static LevelCheckQuestionDto from(CardSnapshot card) {
            return new LevelCheckQuestionDto(
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

    @Schema(name = "LevelCheckAnswersRequest")
    public record LevelCheckAnswersRequest(
            @Schema(description = "Une réponse à chaque question du domaine") @NotNull @Size(min = 1, max = MAX_ANSWERS)
            List<@NotNull @Valid LevelCheckAnswerDto> answers) {}

    @Schema(name = "LevelCheckAnswer")
    public record LevelCheckAnswerDto(
            @NotNull UUID cardId, @NotNull @Valid LevelCheckResponseDto answer) {

        GradedAnswer toAnswer() {
            List<Integer> selected = answer.selectedChoices();
            return new GradedAnswer(cardId, selected == null ? null : Set.copyOf(selected), answer.filledBlanks());
        }
    }

    /** La réponse elle-même, au format du défi du jour : une seule de ses deux formes. */
    @Schema(name = "LevelCheckResponse")
    public record LevelCheckResponseDto(
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

    @Schema(name = "LevelCheckDomainResult")
    public record LevelCheckDomainResultDto(
            @Schema(description = "L'identifiant du domaine")
            String domain,

            @Schema(description = "Vrai si toutes les réponses sont justes : ses notions débutantes sont connues")
            boolean passed,

            @Schema(description = "La correction de chaque réponse, dans l'ordre des réponses")
            List<LevelCheckCorrectionDto> corrections,

            @Schema(description = "Les notions du sujet connues après cette correction")
            int knownNotions) {

        static LevelCheckDomainResultDto from(DomainOutcome outcome) {
            return new LevelCheckDomainResultDto(
                    outcome.domain(),
                    outcome.passed(),
                    outcome.corrections().stream()
                            .map(LevelCheckCorrectionDto::from)
                            .toList(),
                    outcome.knownNotions());
        }
    }

    @Schema(name = "LevelCheckCorrection")
    public record LevelCheckCorrectionDto(
            UUID cardId,
            boolean correct,

            @Schema(description = "Les bonnes réponses d'une carte à choix, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à compléter ; vide sinon")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static LevelCheckCorrectionDto from(AnswerCorrection correction) {
            return new LevelCheckCorrectionDto(
                    correction.cardId(),
                    correction.correct(),
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation());
        }
    }
}
