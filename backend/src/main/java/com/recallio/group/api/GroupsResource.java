package com.recallio.group.api;

import com.recallio.content.CardType;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.application.GroupInvitationService;
import com.recallio.group.application.GroupSettingsService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.application.StudyGroupService.GroupDetail;
import com.recallio.group.application.StudyGroupService.MemberView;
import com.recallio.group.domain.GroupInvitation;
import com.recallio.group.domain.GroupName;
import com.recallio.group.domain.GroupRole;
import com.recallio.group.domain.GroupSettings;
import com.recallio.group.domain.StudyGroupRepository.GroupSummary;
import com.recallio.platform.api.ProblemDetail;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
@Path(GroupsResource.GROUPS)
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class GroupsResource {

    static final String GROUPS = "/api/v1/groups";

    private final StudyGroupService groups;
    private final GroupInvitationService invitations;
    private final GroupSettingsService settings;
    private final CurrentUser currentUser;

    public GroupsResource(
            StudyGroupService groups,
            GroupInvitationService invitations,
            GroupSettingsService settings,
            CurrentUser currentUser) {
        this.groups = groups;
        this.invitations = invitations;
        this.settings = settings;
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
        return Response.created(URI.create(GROUPS + "/" + created.id()))
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

    @PUT
    @Path("/{groupId}/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateGroupSettings", summary = "Remplace les réglages du groupe (créateur seulement)")
    @APIResponse(responseCode = "200", description = "Le groupe et ses nouveaux réglages")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
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
    @APIResponse(
            responseCode = "422",
            description = "Sujet inconnu, ou thème hors des sujets du groupe",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public GroupDto updateGroupSettings(@PathParam("groupId") UUID groupId, @Valid @NotNull GroupSettingsDto request) {
        return GroupDto.from(settings.update(currentUser.get(), groupId, request.toDomain()));
    }

    @POST
    @Path("/{groupId}/invitation")
    @Operation(
            operationId = "renewGroupInvitation",
            summary = "Génère le lien d'invitation du groupe (créateur seulement) ; le précédent cesse de fonctionner")
    @APIResponse(
            responseCode = "201",
            description = "Le nouveau lien",
            content = @Content(schema = @Schema(implementation = GroupInvitationDto.class)))
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
    public Response renewGroupInvitation(@PathParam("groupId") UUID groupId) {
        GroupInvitationDto invitation = GroupInvitationDto.from(invitations.renew(currentUser.get(), groupId));
        return Response.created(URI.create(GROUPS + "/" + groupId + "/invitation"))
                .entity(invitation)
                .build();
    }

    @GET
    @Path("/{groupId}/invitation")
    @Operation(
            operationId = "getGroupInvitation",
            summary = "Le lien d'invitation actif du groupe (créateur seulement)")
    @APIResponse(responseCode = "200", description = "Le lien actif")
    @APIResponse(
            responseCode = "403",
            description = "L'utilisateur n'est pas le créateur",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou sans lien actif",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public GroupInvitationDto getGroupInvitation(@PathParam("groupId") UUID groupId) {
        return GroupInvitationDto.from(invitations.activeInvitation(currentUser.get(), groupId));
    }

    @DELETE
    @Path("/{groupId}/invitation")
    @Operation(operationId = "revokeGroupInvitation", summary = "Désactive le lien d'invitation (créateur seulement)")
    @APIResponse(responseCode = "204", description = "Lien désactivé")
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
    public void revokeGroupInvitation(@PathParam("groupId") UUID groupId) {
        invitations.revoke(currentUser.get(), groupId);
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
            List<GroupMemberDto> members,

            GroupSettingsDto settings) {
        static GroupDto from(GroupDetail group) {
            return new GroupDto(
                    group.id(),
                    group.name(),
                    group.createdAt(),
                    group.myRole(),
                    group.members().stream().map(GroupMemberDto::from).toList(),
                    GroupSettingsDto.from(group.settings()));
        }
    }

    @Schema(name = "GroupSettings")
    public record GroupSettingsDto(
            @Schema(description = "Slugs des sujets du groupe ; aucun : rien à réviser avec le groupe")
            @NotNull
            @Size(max = GroupSettings.MAX_SUBJECTS)
            List<@NotBlank String> subjects,

            @Schema(
                    description = "Slugs de thèmes : restreint chaque sujet aux thèmes listés qui lui"
                            + " appartiennent ; un sujet sans thème listé les garde tous")
            @NotNull
            @Size(max = GroupSettings.MAX_THEMES)
            List<@NotBlank String> themes,

            @Schema(description = "Formats retenus ; aucun : tous les formats") @NotNull @Size(max = 20)
            List<@NotNull CardType> cardTypes,

            @Schema(description = "Cartes à réviser par jour pour qu'une journée compte dans le classement du groupe")
            @Min(GroupSettings.MIN_DAILY_GOAL)
            @Max(GroupSettings.MAX_DAILY_GOAL)
            int dailyGoal,

            @NotNull LeaderboardVisibility leaderboardVisibility) {

        static GroupSettingsDto from(GroupSettings settings) {
            return new GroupSettingsDto(
                    settings.subjectSlugs(),
                    settings.themeSlugs(),
                    settings.cardTypes().stream()
                            .sorted(Comparator.naturalOrder())
                            .toList(),
                    settings.dailyGoal(),
                    settings.leaderboardVisibility());
        }

        GroupSettings toDomain() {
            return new GroupSettings(
                    subjects.stream().map(String::strip).toList(),
                    themes.stream().map(String::strip).toList(),
                    Set.copyOf(cardTypes),
                    dailyGoal,
                    leaderboardVisibility);
        }
    }

    @Schema(name = "GroupInvitation")
    public record GroupInvitationDto(
            @Schema(description = "Le secret du lien, à placer dans l'URL de la page « rejoindre »")
            String token,

            Instant expiresAt) {
        static GroupInvitationDto from(GroupInvitation invitation) {
            return new GroupInvitationDto(invitation.token().value(), invitation.expiresAt());
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
