package com.recallio.notification.api;

import com.recallio.notification.application.PushSubscriptionService;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** La clé publique VAPID du serveur (ADR 0047) : le navigateur la demande pour s'abonner aux rappels. */
@Path("/api/v1/push/public-key")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rappels")
@SecurityRequirement(name = "SecurityScheme")
public class PushPublicKeyResource {

    private final PushSubscriptionService subscriptions;

    public PushPublicKeyResource(PushSubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GET
    @Operation(operationId = "getPushPublicKey", summary = "La clé publique VAPID du serveur, pour s'abonner")
    @APIResponse(responseCode = "200", description = "La clé, P-256 brute en base64url")
    public PushPublicKeyDto getPushPublicKey() {
        return new PushPublicKeyDto(subscriptions.publicKey());
    }

    @Schema(name = "PushPublicKey")
    public record PushPublicKeyDto(String publicKey) {}
}
