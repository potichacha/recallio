package com.recallio.notification.api;

import com.recallio.notification.application.PushSubscriptionService;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Les rappels par notification (ADR 0047), sur acceptation : s'abonner sur un appareil, ou s'en retirer. La clé publique
 * du serveur est servie par {@link PushPublicKeyResource}.
 */
@Path("/api/v1/me/push-subscription")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rappels")
@SecurityRequirement(name = "SecurityScheme")
public class PushSubscriptionsResource {

    static final int MAX_ENDPOINT_LENGTH = 2048;
    static final int MAX_KEY_LENGTH = 200;

    private final PushSubscriptionService subscriptions;
    private final CurrentUser currentUser;

    public PushSubscriptionsResource(PushSubscriptionService subscriptions, CurrentUser currentUser) {
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "subscribeToReminders", summary = "Accepte les rappels sur cet appareil, à cette heure")
    @APIResponse(responseCode = "204", description = "Abonné ; un abonnement déjà connu est mis à jour")
    @APIResponse(responseCode = "422", description = "L'adresse n'est pas celle d'un service de notification connu")
    public void subscribeToReminders(@Valid @NotNull PushSubscriptionRequest request) {
        subscriptions.subscribe(
                currentUser.get(), request.endpoint(), request.p256dh(), request.auth(), request.reminderHour());
    }

    @DELETE
    @Operation(operationId = "unsubscribeFromReminders", summary = "Retire les rappels de cet appareil")
    @APIResponse(responseCode = "204", description = "Désabonné")
    public void unsubscribeFromReminders(
            @QueryParam("endpoint") @NotBlank @Size(max = MAX_ENDPOINT_LENGTH) String endpoint) {
        subscriptions.unsubscribe(currentUser.get(), endpoint);
    }

    @Schema(
            name = "PushSubscriptionRequest",
            requiredProperties = {"endpoint", "p256dh", "auth", "reminderHour"})
    public record PushSubscriptionRequest(
            @Schema(description = "L'adresse donnée par le navigateur") @NotBlank @Size(max = MAX_ENDPOINT_LENGTH)
            String endpoint,

            @Schema(description = "La clé publique de l'appareil, en base64url") @NotBlank @Size(max = MAX_KEY_LENGTH)
            String p256dh,

            @Schema(description = "Le secret d'authentification de l'appareil, en base64url")
            @NotBlank
            @Size(max = MAX_KEY_LENGTH)
            String auth,

            @Schema(description = "L'heure du rappel, de 0 à 23, dans le fuseau de l'utilisateur") @Min(0) @Max(23)
            int reminderHour) {}
}
