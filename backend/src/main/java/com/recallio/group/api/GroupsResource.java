package com.recallio.group.api;

import com.recallio.group.application.StudyGroupService;
import com.recallio.group.application.StudyGroupService.GroupDetail;
import com.recallio.group.application.StudyGroupService.MemberView;
import com.recallio.group.domain.GroupName;
import com.recallio.group.domain.GroupRole;
import com.recallio.group.domain.StudyGroupRepository.GroupSummary;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
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
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Les groupes de l'utilisateur connecté. Un groupe dont il n'est pas membre répond 404, comme un groupe inexistant :
 * son existence ne fuite pas (CLAUDE.md, section 4.5).
 */
@Path("/api/v1/groups")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class GroupsResource {

    private final StudyGroupService groups;
    private final CurrentUser currentUser;

    public GroupsResource(StudyGroupService groups, CurrentUser currentUser) {
        this.groups = groups;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "listMyGroups", summary = "Les groupes dont l'utilisateur est membre")
    public List<GroupSummaryDto> listMyGroups() {
        return groups.myGroups(currentUser.get()).stream()
                .map(GroupSummaryDto::from)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createGroup", summary = "Crée un groupe dont l'utilisateur est le créateur")
    @APIResponse(
            responseCode = "201",
            description = "Le groupe créé",
            content = @Content(schema = @Schema(implementation = GroupDto.class)))
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Trop de groupes créés",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response createGroup(@Valid @NotNull CreateGroupRequest request) {
        GroupDto created = GroupDto.from(groups.create(currentUser.get(), request.name()));
        return Response.created(URI.create("/api/v1/groups/" + created.id()))
                .entity(created)
                .build();
    }

    @GET
    @Path("/{groupId}")
    @Operation(operationId = "getGroup", summary = "Un groupe et ses membres")
    @APIResponse(responseCode = "200", description = "Le groupe")
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public GroupDto getGroup(@PathParam("groupId") UUID groupId) {
        return GroupDto.from(groups.group(currentUser.get(), groupId));
    }

    @DELETE
    @Path("/{groupId}")
    @Operation(operationId = "deleteGroup", summary = "Supprime le groupe (créateur seulement)")
    @APIResponse(responseCode = "204", description = "Groupe supprimé")
    @APIResponse(
            responseCode = "403",
            description = "L'utilisateur n'est pas le créateur",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public void deleteGroup(@PathParam("groupId") UUID groupId) {
        groups.delete(currentUser.get(), groupId);
    }

    @DELETE
    @Path("/{groupId}/members/me")
    @Operation(operationId = "leaveGroup", summary = "Quitte le groupe (sauf le créateur, qui peut le supprimer)")
    @APIResponse(responseCode = "204", description = "Groupe quitté")
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Le créateur ne peut pas quitter son groupe",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public void leaveGroup(@PathParam("groupId") UUID groupId) {
        groups.leave(currentUser.get(), groupId);
    }

    @DELETE
    @Path("/{groupId}/members/{userId}")
    @Operation(operationId = "removeGroupMember", summary = "Retire un membre du groupe (créateur seulement)")
    @APIResponse(responseCode = "204", description = "Membre retiré")
    @APIResponse(
            responseCode = "403",
            description = "L'utilisateur n'est pas le créateur",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Groupe ou membre inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Le créateur ne peut pas être retiré",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public void removeGroupMember(@PathParam("groupId") UUID groupId, @PathParam("userId") UUID userId) {
        groups.removeMember(currentUser.get(), groupId, userId);
    }

    @Schema(name = "CreateGroupRequest")
    public record CreateGroupRequest(
            @NotBlank @Size(max = GroupName.MAX_LENGTH) String name) {}

    @Schema(name = "GroupSummary")
    public record GroupSummaryDto(
            UUID id,
            String name,

            @Schema(description = "Rôle de l'utilisateur connecté dans ce groupe")
            GroupRole myRole,

            int memberCount) {
        static GroupSummaryDto from(GroupSummary group) {
            return new GroupSummaryDto(group.id(), group.name().value(), group.role(), group.memberCount());
        }
    }

    @Schema(name = "Group")
    public record GroupDto(
            UUID id,
            String name,
            Instant createdAt,

            @Schema(description = "Rôle de l'utilisateur connecté dans ce groupe")
            GroupRole myRole,

            @Schema(description = "Le créateur d'abord, puis par ordre d'arrivée")
            List<GroupMemberDto> members) {
        static GroupDto from(GroupDetail group) {
            return new GroupDto(
                    group.id(),
                    group.name(),
                    group.createdAt(),
                    group.myRole(),
                    group.members().stream().map(GroupMemberDto::from).toList());
        }
    }

    @Schema(name = "GroupMember")
    public record GroupMemberDto(
            UUID userId,
            String displayName,
            GroupRole role,
            Instant joinedAt,

            @Schema(description = "Vrai pour l'utilisateur connecté")
            boolean me) {
        static GroupMemberDto from(MemberView member) {
            return new GroupMemberDto(
                    member.userId(), member.displayName(), member.role(), member.joinedAt(), member.me());
        }
    }
}
