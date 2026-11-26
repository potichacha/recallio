package com.recallio.entitlement.api;

import com.recallio.entitlement.application.PlusService;
import com.recallio.entitlement.application.PlusService.CodeOffer;
import com.recallio.entitlement.application.PlusService.PlusStatus;
import com.recallio.entitlement.application.PlusService.Redemption;
import com.recallio.entitlement.domain.AccessCode;
import com.recallio.platform.api.ProblemDetail;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le droit « Plus » de l'utilisateur, et le code d'accès qui l'ouvre (ADR 0079). Le code voyage dans le corps de la
 * requête, jamais dans l'adresse : il n'arrive dans aucun journal.
 */
@Path("/api/v1/me/plus")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Plus")
@SecurityRequirement(name = "SecurityScheme")
public class PlusResource {

    private final PlusService plus;
    private final CurrentUser currentUser;

    public PlusResource(PlusService plus, CurrentUser currentUser) {
        this.plus = plus;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getPlus", summary = "Le droit « Plus » de l'utilisateur : actif ou non, et jusqu'à quand")
    @APIResponse(responseCode = "200", description = "Son droit ; inactif et sans date s'il n'a jamais eu Plus")
    public PlusDto getPlus() {
        return PlusDto.from(plus.status(currentUser.get()));
    }

    @POST
    @Path("/access-code-checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkAccessCode",
            summary = "Dit ce que donnerait un code d'accès, sans l'utiliser ; un essai raté compte")
    @APIResponse(responseCode = "200", description = "Le code marche : ce qu'il donnerait")
    @APIResponse(
            responseCode = "409",
            description = "L'utilisateur a déjà utilisé ce code",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Le code ne marche pas : inconnu, mal formé, épuisé, expiré ou révoqué, sans dire lequel",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "429",
            description = "Trop d'essais ratés dans l'heure",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public AccessCodeOfferDto checkAccessCode(@Valid @NotNull AccessCodeDto request) {
        return AccessCodeOfferDto.from(plus.check(currentUser.get(), request.code()));
    }

    @POST
    @Path("/redemptions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "redeemAccessCode",
            summary = "Utilise un code d'accès : il ouvre le droit « Plus », ou le prolonge")
    @APIResponse(
            responseCode = "201",
            description = "Le code est utilisé : ce qu'il a donné, et le droit après lui",
            content = @Content(schema = @Schema(implementation = RedemptionDto.class)))
    @APIResponse(
            responseCode = "409",
            description = "L'utilisateur a déjà utilisé ce code",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Le code ne marche pas : inconnu, mal formé, épuisé, expiré ou révoqué, sans dire lequel",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "429",
            description = "Trop d'essais ratés dans l'heure",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response redeemAccessCode(@Valid @NotNull AccessCodeDto request) {
        Redemption redemption = plus.redeem(currentUser.get(), request.code());
        return Response.status(Response.Status.CREATED)
                .entity(RedemptionDto.from(redemption))
                .build();
    }

    @Schema(name = "Plus")
    public record PlusDto(
            @Schema(description = "Vrai si l'utilisateur a Plus maintenant")
            boolean active,

            @Schema(
                    description = "La fin de son droit, passée s'il est échu ; absente s'il n'a jamais eu Plus",
                    nullable = true)
            @Nullable
            Instant endsAt,

            @Schema(description = "Les jours qu'il reste, un jour entamé comptant pour un ; 0 sans Plus")
            int daysLeft) {

        static PlusDto from(PlusStatus status) {
            return new PlusDto(status.active(), status.endsAt(), status.daysLeft());
        }
    }

    @Schema(name = "AccessCodeRequest")
    public record AccessCodeDto(
            @Schema(description = "Le code, comme il a été reçu : la casse, les espaces et les tirets ne comptent pas")
            @NotBlank
            @Size(max = AccessCode.MAX_INPUT_LENGTH)
            String code) {

        /** Le code ne s'écrit ni dans un message d'erreur ni dans un log. */
        @Override
        public String toString() {
            return "AccessCodeDto[***]";
        }
    }

    @Schema(name = "AccessCodeOffer")
    public record AccessCodeOfferDto(
            @Schema(description = "Les jours de Plus que le code donnerait")
            int plusDays) {

        static AccessCodeOfferDto from(CodeOffer offer) {
            return new AccessCodeOfferDto(offer.plusDays());
        }
    }

    @Schema(name = "AccessCodeRedemption")
    public record RedemptionDto(
            @Schema(description = "Les jours de Plus que le code a donnés")
            int plusDays,

            @Schema(description = "Le droit de l'utilisateur, après ce code")
            PlusDto plus) {

        static RedemptionDto from(Redemption redemption) {
            return new RedemptionDto(redemption.plusDays(), PlusDto.from(redemption.plus()));
        }
    }
}
