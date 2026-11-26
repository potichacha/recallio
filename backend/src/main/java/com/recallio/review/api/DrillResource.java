package com.recallio.review.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.SubjectScope;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.api.ReviewDtos.ExerciseToSolveDto;
import com.recallio.review.application.AnswerCorrection;
import com.recallio.review.application.DrillService;
import com.recallio.review.application.DrillService.DrillCheck;
import com.recallio.review.application.DrillService.DrillPage;
import com.recallio.review.application.GradedAnswer;
import com.recallio.review.domain.drill.DrillLevel;
import com.recallio.review.domain.drill.DrillQuota;
import com.recallio.review.domain.drill.DrillUsage;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
 * La révision intensive (ADR 0078) : des questions de plusieurs sujets, sans fin, corrigées par le serveur. Les questions
 * et leur correction ont la forme de celles de l'entraînement (ADR 0038), avec le sujet et la notion de chaque carte,
 * pour le bilan que l'écran calcule. Rien n'est enregistré, sauf le compteur du jour, qui porte la limite quotidienne
 * d'un compte sans Plus (ADR 0079).
 */
@Path("/api/v1/me/drill")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Entraînement")
@SecurityRequirement(name = "SecurityScheme")
public class DrillResource {

    static final int MAX_SUBJECTS = 5;
    static final int MAX_PAGE_SIZE = 20;

    /** Bien au-delà de ce qu'une vie de révision atteint, et sans risque de dépassement en ajoutant une page. */
    static final long MAX_OFFSET = 1_000_000_000_000L;

    private static final String DEFAULT_PAGE_SIZE = "10";
    private static final String ADMIN_ROLE = "admin";

    private final DrillService drill;
    private final CurrentUser currentUser;
    private final SecurityIdentity identity;

    public DrillResource(DrillService drill, CurrentUser currentUser, SecurityIdentity identity) {
        this.drill = drill;
        this.currentUser = currentUser;
        this.identity = identity;
    }

    @GET
    @Path("/questions")
    @Operation(
            operationId = "getDrillQuestions",
            summary = "Une page de questions de la révision intensive, dans l'ordre que fixe la graine")
    @APIResponse(
            responseCode = "200",
            description = "Les questions, sans leur corrigé, une par notion au plus ; la page peut être plus courte que"
                    + " demandé")
    @APIResponse(
            responseCode = "400",
            description = "Paramètre absent ou hors bornes",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu, ou en brouillon pour qui n'est pas admin",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public DrillQuestionsDto getDrillQuestions(
            @Parameter(description = "Slugs des sujets à mélanger (répétable), de 1 à 5", required = true)
                    @QueryParam("subjects")
                    @NotNull
                    @Size(min = 1, max = MAX_SUBJECTS)
                    List<@NotBlank String> subjects,
            @Parameter(description = "Le poste visé, qui choisit les notions", required = true)
                    @QueryParam("level")
                    @NotNull
                    DrillLevel level,
            @Parameter(
                            description = "La graine du tirage, choisie par le client au début de la séance : la même"
                                    + " graine donne le même ordre",
                            required = true)
                    @QueryParam("seed")
                    @NotNull
                    Long seed,
            @Parameter(
                            description = "Où reprendre : 0 pour commencer, puis le nextOffset de la page précédente."
                                    + " Passé le total, le tirage repart dans un autre ordre")
                    @QueryParam("offset")
                    @DefaultValue("0")
                    @Min(0)
                    @Max(MAX_OFFSET)
                    long offset,
            @QueryParam("size") @DefaultValue(DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return DrillQuestionsDto.from(
                drill.questions(new LinkedHashSet<>(subjects), level, seed, offset, size, scope()));
    }

    @POST
    @Path("/checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkDrillAnswer",
            summary = "Corrige une réponse de la révision intensive, et la compte dans l'usage du jour (idempotent"
                    + " grâce à requestId)")
    @APIResponse(responseCode = "200", description = "La correction, avec le sujet et la notion de la carte")
    @APIResponse(
            responseCode = "403",
            description = "Sans Plus, la limite quotidienne est atteinte (review.drill-daily-limit-reached)",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "La carte n'est pas une question de la révision intensive : inconnue, retirée, sans choix, ou"
                    + " d'un sujet en brouillon",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "La réponse ne convient pas à la carte",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public DrillCorrectionDto checkDrillAnswer(@Valid @NotNull DrillAnswerDto answer) {
        return DrillCorrectionDto.from(drill.check(currentUser.get(), answer.requestId(), answer.toAnswer(), scope()));
    }

    @GET
    @Path("/usage")
    @Operation(
            operationId = "getDrillUsage",
            summary = "Les réponses de la révision intensive corrigées aujourd'hui, au jour de l'utilisateur, et ce"
                    + " qu'il lui reste sans Plus")
    @APIResponse(responseCode = "200", description = "L'usage du jour ; à zéro sans réponse")
    public DrillUsageDto getDrillUsage() {
        return DrillUsageDto.from(drill.quota(currentUser.get()));
    }

    /** Un admin s'entraîne aussi sur les brouillons, pour les relire en conditions réelles. */
    private SubjectScope scope() {
        return identity.hasRole(ADMIN_ROLE) ? SubjectScope.WITH_DRAFTS : SubjectScope.PUBLISHED;
    }

    @Schema(name = "DrillQuestions")
    public record DrillQuestionsDto(
            @Schema(description = "Les questions de la page, dans l'ordre où les poser, une par notion au plus")
            List<DrillQuestionDto> questions,

            @Schema(description = "Le nombre de cartes éligibles pour ces sujets et ce poste ; 0 s'il n'y en a aucune")
            int total,

            @Schema(description = "L'offset à demander pour la page suivante")
            long nextOffset) {

        static DrillQuestionsDto from(DrillPage page) {
            return new DrillQuestionsDto(
                    page.questions().stream().map(DrillQuestionDto::from).toList(), page.total(), page.nextOffset());
        }
    }

    /** Une carte de l'entraînement ({@code PracticeCard}), avec son sujet ; sa notion est toujours là. */
    @Schema(name = "DrillQuestion")
    public record DrillQuestionDto(
            UUID cardId,
            CardType type,

            @Schema(description = "Recto, en Markdown ; l'explication arrive avec la correction")
            String prompt,

            @Schema(description = "Les choix de la carte, sans dire lesquels sont justes (ADR 0051)")
            List<String> choices,

            @Schema(description = "Vrai si plusieurs choix sont justes")
            boolean multipleAnswers,

            @Schema(description = "Toujours 0 : la révision intensive ne pose pas de code à trous")
            int blankCount,

            @Schema(description = "Toujours absent : la révision intensive ne pose pas d'exercice", nullable = true)
            @Nullable
            ExerciseToSolveDto exercise,

            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Le slug de la notion de la carte, qui mène à sa leçon")
            String notionSlug,

            String notionTitle) {

        static DrillQuestionDto from(CardSnapshot card) {
            return new DrillQuestionDto(
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size(),
                    null,
                    card.subjectSlug(),
                    card.subjectTitle(),
                    Objects.requireNonNull(card.notionSlug()),
                    Objects.requireNonNull(card.notionTitle()));
        }
    }

    /** Une réponse de l'entraînement ({@code PracticeAnswer}), avec l'identifiant qui la rend idempotente. */
    @Schema(name = "DrillAnswer")
    public record DrillAnswerDto(
            @Schema(
                    description =
                            "Identifiant généré par le client pour cette réponse, réutilisé tel quel si la requête"
                                    + " est renvoyée : elle n'est alors pas comptée deux fois")
            @NotNull
            UUID requestId,

            @NotNull UUID cardId,

            @Schema(description = "Les positions cochées, à partir de 0", nullable = true) @Nullable
            List<@NotNull Integer> selectedChoices,

            @Schema(description = "Refusé : la révision intensive ne pose pas de code à trous", nullable = true)
            @Nullable
            List<@NotNull String> filledBlanks) {

        GradedAnswer toAnswer() {
            List<Integer> selected = selectedChoices;
            return new GradedAnswer(cardId, selected == null ? null : Set.copyOf(selected), filledBlanks);
        }
    }

    /** Une correction de l'entraînement ({@code PracticeCorrection}), avec de quoi tenir le bilan et l'usage du jour. */
    @Schema(name = "DrillCorrection")
    public record DrillCorrectionDto(
            UUID cardId,
            boolean correct,

            @Schema(description = "Les bonnes réponses, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "Toujours vide : la révision intensive ne pose pas de code à trous")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation,

            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Le slug de la notion de la carte : avec celui du sujet, il mène à sa leçon")
            String notionSlug,

            String notionTitle,

            @Schema(description = "Les réponses corrigées aujourd'hui, celle-ci comprise")
            int answersToday,

            @Schema(
                    description = "Les réponses qu'il reste aujourd'hui sans Plus ; absent avec Plus, qui n'a pas de"
                            + " limite",
                    nullable = true)
            @Nullable
            Integer remainingToday) {

        static DrillCorrectionDto from(DrillCheck check) {
            AnswerCorrection correction = check.correction();
            CardSnapshot card = check.card();
            return new DrillCorrectionDto(
                    correction.cardId(),
                    correction.correct(),
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation(),
                    card.subjectSlug(),
                    card.subjectTitle(),
                    Objects.requireNonNull(card.notionSlug()),
                    Objects.requireNonNull(card.notionTitle()),
                    check.quota().usage().answers(),
                    check.quota().remaining());
        }
    }

    @Schema(name = "DrillUsage")
    public record DrillUsageDto(
            @Schema(description = "Le jour de l'utilisateur, dans son fuseau")
            LocalDate day,

            @Schema(description = "Les réponses corrigées ce jour-là")
            int answers,

            @Schema(description = "Celles qui étaient justes")
            int correct,

            @Schema(
                    description = "Le nombre de réponses par jour sans Plus ; absent avec Plus, qui n'a pas de limite",
                    nullable = true)
            @Nullable
            Integer dailyLimit,

            @Schema(description = "Les réponses qu'il reste aujourd'hui ; absent avec Plus", nullable = true) @Nullable
            Integer remaining) {

        static DrillUsageDto from(DrillQuota quota) {
            DrillUsage usage = quota.usage();
            return new DrillUsageDto(
                    usage.day(), usage.answers(), usage.correct(), quota.dailyLimit(), quota.remaining());
        }
    }
}
