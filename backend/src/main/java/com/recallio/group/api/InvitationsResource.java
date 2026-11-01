package com.recallio.group.api;

import com.recallio.group.api.GroupsResource.GroupDto;
import com.recallio.group.application.GroupInvitationService;
import com.recallio.group.application.GroupInvitationService.InvitationPreview;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Le côté invité d'un lien d'invitation : voir de quel groupe il s'agit, puis le rejoindre. Il faut être connecté, et un
 * lien inconnu, expiré ou remplacé répond 404 sans rien révéler du groupe.
 */
@Path("/api/v1/invitations/{token}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class InvitationsResource {

    private final GroupInvitationService invitations;
    private final CurrentUser currentUser;

    public InvitationsResource(GroupInvitationService invitations, CurrentUser currentUser) {
        this.invitations = invitations;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getInvitation", summary = "Le groupe auquel ce lien invite")
    @APIResponse(responseCode = "200", description = "L'invitation")
    @APIResponse(
            responseCode = "404",
            description = "Lien inconnu, expiré ou remplacé",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public InvitationDto getInvitation(@PathParam("token") String token) {
        return InvitationDto.from(invitations.preview(currentUser.get(), token));
    }

    @POST
    @Path("/accept")
    @Operation(operationId = "acceptInvitation", summary = "Rejoint le groupe ; sans effet pour qui en est déjà membre")
    @APIResponse(responseCode = "200", description = "Le groupe rejoint")
    @APIResponse(
            responseCode = "404",
            description = "Lien inconnu, expiré ou remplacé",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Le groupe est complet",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public GroupDto acceptInvitation(@PathParam("token") String token) {
        return GroupDto.from(invitations.accept(currentUser.get(), token));
    }

    @Schema(name = "Invitation")
    public record InvitationDto(
            UUID groupId,
            String groupName,
            int memberCount,
            Instant expiresAt,

            @Schema(description = "Vrai si l'utilisateur connecté fait déjà partie du groupe")
            boolean alreadyMember) {
        static InvitationDto from(InvitationPreview preview) {
            return new InvitationDto(
                    preview.groupId(),
                    preview.groupName(),
                    preview.memberCount(),
                    preview.expiresAt(),
                    preview.alreadyMember());
        }
    }
}
