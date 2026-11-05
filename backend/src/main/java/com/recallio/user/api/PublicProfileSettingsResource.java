package com.recallio.user.api;

import com.recallio.user.application.PublicProfileService;
import com.recallio.user.domain.PublicHandle;
import com.recallio.user.domain.PublicProfileSettings;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** L'adresse et la visibilité du profil public de l'utilisateur connecté (ADR 0043), privé par défaut. */
@Path("/api/v1/me/public-profile")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Profil")
@SecurityRequirement(name = "SecurityScheme")
public class PublicProfileSettingsResource {

    private final PublicProfileService publicProfiles;
    private final TokenIdentity identity;

    public PublicProfileSettingsResource(PublicProfileService publicProfiles, TokenIdentity identity) {
        this.publicProfiles = publicProfiles;
        this.identity = identity;
    }

    @GET
    @Operation(operationId = "getMyPublicProfileSettings", summary = "L'adresse du profil public, et s'il est visible")
    @APIResponse(responseCode = "200", description = "Les réglages ; sans adresse tant qu'aucune n'est choisie")
    public PublicProfileSettingsDto getMyPublicProfileSettings() {
        return PublicProfileSettingsDto.from(publicProfiles.settings(identity.current()));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "updateMyPublicProfileSettings",
            summary = "Choisit l'adresse du profil public, et le rend public ou privé")
    @APIResponse(responseCode = "200", description = "Les réglages enregistrés")
    @APIResponse(responseCode = "409", description = "Un autre utilisateur a déjà cette adresse")
    @APIResponse(responseCode = "422", description = "Adresse mal formée")
    public PublicProfileSettingsDto updateMyPublicProfileSettings(@Valid @NotNull UpdatePublicProfileRequest request) {
        return PublicProfileSettingsDto.from(
                Optional.of(publicProfiles.update(identity.current(), request.handle(), request.visible())));
    }

    @Schema(name = "PublicProfileSettings")
    public record PublicProfileSettingsDto(
            @Schema(description = "L'adresse du profil, /u/<handle> ; absente tant qu'aucune n'est choisie") @Nullable
            String handle,

            @Schema(description = "Faux par défaut : le profil est privé")
            boolean visible) {

        static PublicProfileSettingsDto from(Optional<PublicProfileSettings> settings) {
            return settings.map(
                            found -> new PublicProfileSettingsDto(found.handle().value(), found.visible()))
                    .orElseGet(() -> new PublicProfileSettingsDto(null, false));
        }
    }

    @Schema(
            name = "UpdatePublicProfileRequest",
            requiredProperties = {"handle", "visible"})
    public record UpdatePublicProfileRequest(
            @Schema(description = "De 3 à 30 caractères : minuscules, chiffres et tirets")
            @NotBlank
            @Size(max = PublicHandle.MAX_LENGTH)
            String handle,

            boolean visible) {}
}
