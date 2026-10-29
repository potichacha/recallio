package com.recallio.user.api;

import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.application.UserProfileService;
import com.recallio.user.domain.DisplayName;
import com.recallio.user.domain.UserProfile;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** L'utilisateur connecté : chacun ne voit et ne modifie que son propre profil (CLAUDE.md, section 4.5). */
@Path("/api/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Profil")
@SecurityRequirement(name = "SecurityScheme")
public class MeResource {

    static final String TIMEZONE_HEADER = "X-Timezone";

    private final UserProfileService profiles;
    private final TokenIdentity identity;
    private final SecurityIdentity securityIdentity;

    public MeResource(UserProfileService profiles, TokenIdentity identity, SecurityIdentity securityIdentity) {
        this.profiles = profiles;
        this.identity = identity;
        this.securityIdentity = securityIdentity;
    }

    @GET
    @Operation(operationId = "getMe", summary = "Profil de l'utilisateur connecté, créé à la première connexion")
    @APIResponse(responseCode = "200", description = "Le profil")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public ProfileDto getMe(
            @Parameter(description = "Fuseau du navigateur (IANA), utilisé seulement à la création du profil")
                    @HeaderParam(TIMEZONE_HEADER)
                    @Nullable
                    String browserTimezone) {
        return toDto(profiles.currentProfile(identity.current(), Optional.ofNullable(browserTimezone)));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateMe", summary = "Met à jour le nom affiché et le fuseau horaire")
    @APIResponse(responseCode = "200", description = "Le profil mis à jour")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Fuseau horaire inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ProfileDto updateMe(@Valid @NotNull UpdateProfileRequest request) {
        return toDto(profiles.updatePreferences(identity.current(), request.displayName(), request.timezone()));
    }

    private ProfileDto toDto(UserProfile profile) {
        List<String> roles = securityIdentity.getRoles().stream().sorted().toList();
        return new ProfileDto(
                profile.id(), profile.displayName().value(), profile.timezone().getId(), roles);
    }

    @Schema(name = "Profile")
    public record ProfileDto(
            UUID id,
            String displayName,

            @Schema(description = "Fuseau IANA, par exemple Europe/Paris")
            String timezone,

            @Schema(description = "Rôles applicatifs, par exemple admin")
            List<String> roles) {}

    @Schema(name = "UpdateProfileRequest")
    public record UpdateProfileRequest(
            @NotBlank @Size(max = DisplayName.MAX_LENGTH) String displayName,
            @NotBlank String timezone) {}
}
