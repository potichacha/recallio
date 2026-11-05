package com.recallio.ranked.api;

import com.recallio.ranked.api.RankedDtos.AnswerResultDto;
import com.recallio.ranked.api.RankedDtos.DuelDto;
import com.recallio.ranked.api.RankedDtos.LiveQueueDto;
import com.recallio.ranked.api.RankedDtos.TurnDto;
import com.recallio.ranked.application.DuelService;
import com.recallio.ranked.application.RankedViews;
import com.recallio.ranked.domain.Duel;
import com.recallio.user.CurrentUser;
import com.recallio.user.UserRef;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** Les duels du classé (ADR 0046) : en chercher un, en asynchrone ou en direct, puis le jouer. */
@Path("/api/v1/ranked")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Classé")
@SecurityRequirement(name = "SecurityScheme")
public class DuelsResource {

    static final int MAX_SLUG_LENGTH = 100;

    private final DuelService duels;
    private final RankedViews views;
    private final CurrentUser currentUser;

    public DuelsResource(DuelService duels, RankedViews views, CurrentUser currentUser) {
        this.duels = duels;
        this.views = views;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/duels")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "findDuel",
            summary =
                    "Cherche un duel asynchrone : le sien s'il n'est pas fini, sinon un duel ouvert, sinon un nouveau")
    @APIResponse(responseCode = "200", description = "Le duel à jouer")
    @APIResponse(responseCode = "404", description = "Sujet inconnu ou non publié")
    @APIResponse(responseCode = "422", description = "Pas assez de questions corrigées par le serveur dans ce sujet")
    public DuelDto findDuel(@Valid @NotNull SubjectRequest request) {
        UserRef user = currentUser.get();
        return DuelDto.from(views.view(duels.findAsync(user, request.subject()), user));
    }

    @POST
    @Path("/live-queue")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "queueLiveDuel",
            summary = "Le classé en direct : entre dans la file, s'y signale, ou trouve son duel. À rappeler toutes les"
                    + " quelques secondes")
    @APIResponse(responseCode = "200", description = "En attente, ou le duel trouvé")
    public LiveQueueDto queueLiveDuel(@Valid @NotNull SubjectRequest request) {
        Optional<UUID> duel = duels.queueLive(currentUser.get(), request.subject());
        return new LiveQueueDto(duel.isEmpty(), duel.orElse(null));
    }

    @DELETE
    @Path("/live-queue")
    @Operation(operationId = "leaveLiveQueue", summary = "Quitte la file du classé en direct")
    @APIResponse(responseCode = "204", description = "Hors de la file")
    public void leaveLiveQueue() {
        duels.leaveLive(currentUser.get());
    }

    @GET
    @Path("/duels/{id}")
    @Operation(
            operationId = "getDuel",
            summary = "Un duel : où en sont les deux joueurs, et le résultat une fois réglé")
    @APIResponse(responseCode = "200", description = "Le duel")
    @APIResponse(responseCode = "404", description = "Aucun duel de cet identifiant parmi ceux de l'utilisateur")
    public DuelDto getDuel(@PathParam("id") UUID id) {
        UserRef user = currentUser.get();
        return DuelDto.from(views.view(duels.view(user, id), user));
    }

    @POST
    @Path("/duels/{id}/next")
    @Operation(
            operationId = "nextDuelQuestion",
            summary = "La question suivante, servie une seule fois : son temps compte dès cet instant")
    @APIResponse(responseCode = "200", description = "La question et son temps, ou la fin du duel pour ce joueur")
    @APIResponse(responseCode = "409", description = "Le duel n'attend plus de réponse de ce joueur")
    public TurnDto nextDuelQuestion(@PathParam("id") UUID id) {
        return duels.next(currentUser.get(), id).map(TurnDto::from).orElseGet(() -> TurnDto.finished(Duel.QUESTIONS));
    }

    @POST
    @Path("/duels/{id}/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "answerDuelQuestion", summary = "Répond à la question servie : la correction")
    @APIResponse(responseCode = "200", description = "La correction ; une réponse en retard compte fausse")
    @APIResponse(responseCode = "409", description = "Cette question n'attend pas de réponse")
    @APIResponse(responseCode = "422", description = "La réponse ne convient pas au format de la question")
    public AnswerResultDto answerDuelQuestion(@PathParam("id") UUID id, @Valid @NotNull DuelAnswerRequest request) {
        List<Integer> selected = request.selectedChoices();
        return AnswerResultDto.from(duels.answer(
                currentUser.get(),
                id,
                request.position(),
                selected == null ? null : Set.copyOf(selected),
                request.filledBlanks()));
    }

    @Schema(name = "RankedSubjectRequest")
    public record SubjectRequest(
            @NotBlank @Size(max = MAX_SLUG_LENGTH) String subject) {}

    @Schema(
            name = "DuelAnswerRequest",
            requiredProperties = {"position"})
    public record DuelAnswerRequest(
            @Schema(description = "Le rang de la question servie, à partir de 0") @Min(0) @Max(Duel.QUESTIONS - 1)
            int position,

            @Schema(description = "Les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0", nullable = true)
            @Nullable
            @Size(max = 8)
            List<@NotNull Integer> selectedChoices,

            @Schema(description = "Les réponses d'un code à trous, une par trou", nullable = true)
            @Nullable
            @Size(max = 20)
            List<@NotNull String> filledBlanks) {}
}
