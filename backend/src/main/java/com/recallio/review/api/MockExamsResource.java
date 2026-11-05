package com.recallio.review.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.application.AnswerCorrection;
import com.recallio.review.application.GradedAnswer;
import com.recallio.review.application.MockExamService;
import com.recallio.review.application.MockExamService.ExamResult;
import com.recallio.review.application.MockExamService.ExamView;
import com.recallio.review.domain.exam.MockExam;
import com.recallio.review.domain.exam.MockExam.Submission;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** L'examen blanc d'un sujet (ADR 0044) : le commencer, le reprendre, rendre sa copie, revoir ses notes. */
@Path("/api/v1/me/mock-exams")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Examen blanc")
@SecurityRequirement(name = "SecurityScheme")
public class MockExamsResource {

    static final int MAX_SLUG_LENGTH = 100;

    private final MockExamService exams;
    private final CurrentUser currentUser;

    public MockExamsResource(MockExamService exams, CurrentUser currentUser) {
        this.exams = exams;
        this.currentUser = currentUser;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "startMockExam", summary = "Commence un examen blanc sur un sujet")
    @APIResponse(
            responseCode = "201",
            description = "L'examen, ses questions sans leurs réponses et le temps imparti",
            content = @Content(schema = @Schema(implementation = MockExamDto.class)))
    @APIResponse(responseCode = "404", description = "Sujet inconnu ou non publié")
    @APIResponse(responseCode = "422", description = "Pas assez de questions corrigées par le serveur dans ce sujet")
    public Response startMockExam(@Valid @NotNull StartMockExamRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(MockExamDto.from(exams.start(currentUser.get(), request.subject())))
                .build();
    }

    @GET
    @Operation(operationId = "listMockExams", summary = "Les dernières copies rendues dans un sujet")
    @APIResponse(responseCode = "200", description = "La plus récente d'abord")
    public List<MockExamSummaryDto> listMockExams(
            @QueryParam("subject") @NotBlank @Size(max = MAX_SLUG_LENGTH) String subject) {
        return exams.history(currentUser.get(), subject).stream()
                .map(MockExamSummaryDto::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getMockExam", summary = "Un examen blanc, pour le reprendre")
    @APIResponse(responseCode = "200", description = "L'examen et le temps qui reste")
    @APIResponse(responseCode = "404", description = "Aucun examen de cet identifiant parmi ceux de l'utilisateur")
    public MockExamDto getMockExam(@PathParam("id") UUID id) {
        return MockExamDto.from(exams.get(currentUser.get(), id));
    }

    @POST
    @Path("/{id}/submission")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "submitMockExam", summary = "Rend la copie : la note et le corrigé")
    @APIResponse(responseCode = "200", description = "La note et la correction de chaque question")
    @APIResponse(responseCode = "404", description = "Aucun examen de cet identifiant parmi ceux de l'utilisateur")
    @APIResponse(responseCode = "409", description = "Copie déjà rendue")
    @APIResponse(responseCode = "422", description = "Une réponse ne convient pas au format de sa question")
    public MockExamResultDto submitMockExam(
            @PathParam("id") UUID id, @Valid @NotNull MockExamSubmissionRequest request) {
        List<GradedAnswer> answers =
                request.answers().stream().map(ExamAnswerDto::toAnswer).toList();
        return MockExamResultDto.from(exams.submit(currentUser.get(), id, answers));
    }

    @Schema(name = "StartMockExamRequest")
    public record StartMockExamRequest(
            @NotBlank @Size(max = MAX_SLUG_LENGTH) String subject) {}

    @Schema(name = "MockExam")
    public record MockExamDto(
            UUID id,
            String subject,
            Instant startedAt,

            @Schema(description = "Le temps qui reste, en millisecondes, mesuré par le serveur ; 0 une fois écoulé")
            long remainingMs,

            @Schema(description = "Vrai si la copie est rendue")
            boolean submitted,

            @Schema(description = "Les questions, dans l'ordre, sans leurs réponses")
            List<ExamQuestionDto> questions) {

        static MockExamDto from(ExamView view) {
            MockExam exam = view.exam();
            return new MockExamDto(
                    exam.id(),
                    exam.subjectSlug(),
                    exam.startedAt(),
                    view.remaining().toMillis(),
                    exam.isSubmitted(),
                    view.questions().stream().map(ExamQuestionDto::from).toList());
        }
    }

    @Schema(name = "ExamQuestion")
    public record ExamQuestionDto(
            UUID cardId,
            CardType type,

            @Schema(description = "En Markdown ; les trous d'un code à compléter sont des ___")
            String prompt,

            @Schema(description = "Les choix d'un QCM ou d'un vrai ou faux, sans dire lesquels sont justes")
            List<String> choices,

            @Schema(description = "Vrai si plusieurs choix sont justes")
            boolean multipleAnswers,

            @Schema(description = "Le nombre de trous d'un code à compléter")
            int blankCount) {

        static ExamQuestionDto from(CardSnapshot card) {
            return new ExamQuestionDto(
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size());
        }
    }

    @Schema(name = "MockExamSubmissionRequest")
    public record MockExamSubmissionRequest(
            @Schema(description = "Au plus une réponse par question ; une question sans réponse est fausse")
            @NotNull
            @Size(max = MockExam.QUESTIONS)
            List<@NotNull @Valid ExamAnswerDto> answers) {}

    @Schema(name = "ExamAnswer")
    public record ExamAnswerDto(
            @NotNull UUID cardId,

            @Schema(description = "Les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0", nullable = true)
            @Nullable
            List<@NotNull Integer> selectedChoices,

            @Schema(description = "Les réponses d'un code à trous, une par trou", nullable = true) @Nullable
            List<@NotNull String> filledBlanks) {

        GradedAnswer toAnswer() {
            List<Integer> selected = selectedChoices;
            return new GradedAnswer(cardId, selected == null ? null : Set.copyOf(selected), filledBlanks);
        }
    }

    @Schema(name = "MockExamResult")
    public record MockExamResultDto(
            @Schema(description = "Les questions justes") int correct,
            @Schema(description = "Les questions notées") int total,
            @Schema(description = "La note, de 0 à 100") int percent,

            @Schema(description = "Faux si la copie est arrivée après le temps imparti")
            boolean inTime,

            @Schema(description = "La correction de chaque question, dans l'ordre de l'examen")
            List<ExamQuestionResultDto> results) {

        static MockExamResultDto from(ExamResult result) {
            Submission submission = result.submission();
            return new MockExamResultDto(
                    submission.correct(),
                    submission.total(),
                    submission.percent(),
                    result.inTime(),
                    result.corrections().stream()
                            .map(ExamQuestionResultDto::from)
                            .toList());
        }
    }

    @Schema(name = "ExamQuestionResult")
    public record ExamQuestionResultDto(
            UUID cardId,
            boolean correct,

            @Schema(description = "Les bonnes réponses d'un QCM ou d'un vrai ou faux, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à compléter")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static ExamQuestionResultDto from(AnswerCorrection correction) {
            return new ExamQuestionResultDto(
                    correction.cardId(),
                    correction.correct(),
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation());
        }
    }

    @Schema(name = "MockExamSummary")
    public record MockExamSummaryDto(UUID id, Instant startedAt, int correct, int total, int percent, boolean inTime) {

        static MockExamSummaryDto from(MockExam exam) {
            Submission submission = exam.submission();
            if (submission == null) {
                throw new IllegalArgumentException("Seules les copies rendues ont une note : " + exam.id());
            }
            return new MockExamSummaryDto(
                    exam.id(),
                    exam.startedAt(),
                    submission.correct(),
                    submission.total(),
                    submission.percent(),
                    exam.inTime(submission.submittedAt()));
        }
    }
}
