package com.recallio.review.api;

import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.application.InterviewService;
import com.recallio.review.application.InterviewService.SubjectCountdown;
import com.recallio.review.domain.interview.InterviewCountdown;
import com.recallio.review.domain.interview.InterviewCountdown.PlanNotion;
import com.recallio.review.domain.interview.InterviewCountdown.PlannedWeek;
import com.recallio.review.domain.interview.InterviewCountdown.ThisWeek;
import com.recallio.review.domain.interview.InterviewStatus;
import com.recallio.review.domain.track.MasteryLevel;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Le compte à rebours avant un entretien (ADR 0066), une date par sujet suivi : chacun ne voit et ne modifie que les
 * siennes (CLAUDE.md, section 4.5).
 */
@Path("/api/v1/me/interviews")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Révision")
@SecurityRequirement(name = "SecurityScheme")
public class InterviewsResource {

    private final InterviewService interviews;
    private final CurrentUser currentUser;

    public InterviewsResource(InterviewService interviews, CurrentUser currentUser) {
        this.interviews = interviews;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getMyInterviews",
            summary = "Les comptes à rebours des sujets suivis qui ont une date d'entretien, dans l'ordre du catalogue")
    @APIResponse(responseCode = "200", description = "Les comptes à rebours")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public List<InterviewCountdownDto> getMyInterviews() {
        return interviews.countdowns(currentUser.get()).stream()
                .map(InterviewCountdownDto::from)
                .toList();
    }

    @PUT
    @Path("/{subject}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "planInterview",
            summary = "Fixe ou change la date de l'entretien d'un sujet suivi : le plan repart d'aujourd'hui")
    @APIResponse(responseCode = "200", description = "Le compte à rebours du sujet")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Date pas après aujourd'hui ou à plus d'un an, sujet non suivi, ou sans notion essentielle",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public InterviewCountdownDto planInterview(
            @Parameter(description = "Slug du sujet suivi") @PathParam("subject") String subject,
            @Valid @NotNull InterviewDateRequest request) {
        return InterviewCountdownDto.from(interviews.plan(currentUser.get(), subject, request.date()));
    }

    @DELETE
    @Path("/{subject}")
    @Operation(operationId = "cancelInterview", summary = "Retire la date de l'entretien d'un sujet")
    @APIResponse(responseCode = "204", description = "Date retirée, ou il n'y en avait pas")
    public void cancelInterview(@Parameter(description = "Slug du sujet") @PathParam("subject") String subject) {
        interviews.cancel(currentUser.get(), subject);
    }

    @Schema(name = "InterviewDateRequest")
    public record InterviewDateRequest(
            @Schema(description = "Le jour de l'entretien, après aujourd'hui et dans l'année", example = "2026-12-15")
            @NotNull
            LocalDate date) {}

    @Schema(
            name = "InterviewCountdown",
            description = "Le compte à rebours avant l'entretien d'un sujet : les notions essentielles à acquérir,"
                    + " réparties par semaine, et l'avance ou le retard")
    public record InterviewCountdownDto(
            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Le jour de l'entretien") LocalDate date,

            @Schema(description = "Les jours jusqu'à l'entretien : 0 le jour même, et après")
            int daysLeft,

            InterviewStatus status,

            @Schema(description = "L'indice de préparation du sujet, de 0 à 100 (ADR 0041)")
            int readinessPercent,

            @Schema(description = "La semaine en cours, à partir de 1, comptée depuis le jour où la date a été fixée")
            int week,

            @Schema(description = "Les semaines jusqu'à l'entretien")
            int weeks,

            @Schema(description = "Vrai pendant la dernière semaine, qui sert à réviser, sans nouvelle notion")
            boolean reviewWeek,

            InterviewWeekDto thisWeek,

            @Schema(description = "Les semaines suivantes, dans l'ordre")
            List<UpcomingInterviewWeekDto> nextWeeks,

            @Schema(description = "Les notions essentielles pas encore acquises")
            int essentialsLeft,

            @Schema(description = "Toutes les notions essentielles du sujet")
            int essentials) {

        static InterviewCountdownDto from(SubjectCountdown subject) {
            InterviewCountdown countdown = subject.countdown();
            return new InterviewCountdownDto(
                    countdown.plan().subjectSlug(),
                    subject.subjectTitle(),
                    countdown.plan().interviewOn(),
                    countdown.daysLeft(),
                    countdown.status(),
                    countdown.readiness().percent(),
                    countdown.week(),
                    countdown.weeks(),
                    countdown.thisWeek().review(),
                    InterviewWeekDto.from(countdown.thisWeek()),
                    countdown.nextWeeks().stream()
                            .map(UpcomingInterviewWeekDto::from)
                            .toList(),
                    countdown.essentialsLeft(),
                    countdown.readiness().essentials());
        }
    }

    @Schema(name = "InterviewWeek", description = "La semaine en cours du plan")
    public record InterviewWeekDto(
            LocalDate startsOn,

            @Schema(description = "Le dernier jour de la semaine, au plus tard la veille de l'entretien")
            LocalDate endsOn,

            @Schema(
                    description =
                            "Les notions à acquérir d'ici la fin de la semaine pour tenir le plan, retard compris")
            int quota,

            @Schema(description = "Ces notions, dans l'ordre du parcours « préparer un entretien »")
            List<InterviewNotionDto> notions) {

        static InterviewWeekDto from(ThisWeek week) {
            return new InterviewWeekDto(
                    week.startsOn(),
                    week.endsOn(),
                    week.quota(),
                    week.notions().stream().map(InterviewNotionDto::from).toList());
        }
    }

    @Schema(name = "UpcomingInterviewWeek", description = "Une semaine à venir du plan")
    public record UpcomingInterviewWeekDto(
            @Schema(description = "Son rang dans le plan, à partir de 1")
            int number,

            LocalDate startsOn,
            LocalDate endsOn,

            @Schema(description = "Les notions que le plan y prévoit")
            int notions,

            @Schema(description = "Vrai pour la dernière semaine, qui sert à réviser")
            boolean review) {

        static UpcomingInterviewWeekDto from(PlannedWeek week) {
            return new UpcomingInterviewWeekDto(
                    week.number(), week.startsOn(), week.endsOn(), week.notions(), week.review());
        }
    }

    @Schema(name = "InterviewNotion", description = "Une notion essentielle à acquérir")
    public record InterviewNotionDto(
            String slug,
            String title,

            @Schema(description = "Son niveau de maîtrise, en dessous d'« acquise » (ADR 0034)")
            MasteryLevel mastery) {

        static InterviewNotionDto from(PlanNotion notion) {
            return new InterviewNotionDto(notion.slug(), notion.title(), notion.mastery());
        }
    }
}
