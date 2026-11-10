package com.recallio.review.api;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.TourPage;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.ExerciseFileDto;
import com.recallio.review.application.PlaygroundRunService;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramStatus;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Lance un programme du Tour et renvoie ce qu'il a écrit (ADR 0037) : l'exemple d'une page, ou ce qu'en a fait
 * l'utilisateur. Rien n'est enregistré. Réservé aux utilisateurs connectés, une exécution à la fois chacun.
 */
@Path("/api/v1/playground-runs")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tour")
@SecurityRequirement(name = "SecurityScheme")
public class PlaygroundResource {

    private final PlaygroundRunService runs;
    private final CurrentUser currentUser;

    public PlaygroundResource(PlaygroundRunService runs, CurrentUser currentUser) {
        this.runs = runs;
        this.currentUser = currentUser;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "runProgram",
            summary = "Lance un programme du Tour et renvoie ses sorties : rien n'est enregistré")
    @APIResponse(responseCode = "200", description = "Ce que le programme a écrit, et comment il s'est terminé")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide : aucun fichier, trop de fichiers, un chemin invalide ou un fichier vide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Les fichiers ne forment pas un programme : deux langages, un fichier de tests, "
                    + "ou pas de Main.java en Java",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "429",
            description = "Une exécution de cet utilisateur est déjà en cours",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "503",
            description = "L'exécution du code est indisponible",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ProgramRunDto runProgram(@Valid @NotNull PlaygroundRunRequest request) {
        List<SourceFile> files =
                request.files().stream().map(ExerciseFileDto::toSourceFile).toList();
        return ProgramRunDto.from(runs.run(currentUser.get(), files));
    }

    @Schema(name = "PlaygroundRunRequest")
    public record PlaygroundRunRequest(
            @Schema(
                    description = "Les fichiers du programme : en Java, un Main.java ; en Go, un package main ; "
                            + "en C++, des .cpp et des .hpp, dont une fonction main")
            @NotNull
            @Size(min = 1, max = TourPage.MAX_FILES)
            List<@NotNull @Valid ExerciseFileDto> files) {}

    @Schema(name = "ProgramRun")
    public record ProgramRunDto(
            ProgramStatus status,

            @Schema(description = "Le code de sortie du programme ; 0 s'il n'a pas été lancé")
            int exitCode,

            String stdout,
            String stderr,

            @Schema(description = "Vrai si l'une des deux sorties a été tronquée (64 Kio au plus chacune)")
            boolean truncated,

            @Schema(description = "Les erreurs de compilation, ou ce qui a arrêté le programme ; vide sinon")
            String message) {

        static ProgramRunDto from(ProgramRun run) {
            return new ProgramRunDto(
                    run.status(), run.exitCode(), run.stdout(), run.stderr(), run.truncated(), run.message());
        }
    }
}
