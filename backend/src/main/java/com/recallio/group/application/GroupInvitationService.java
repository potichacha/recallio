package com.recallio.group.application;

import com.recallio.group.application.StudyGroupService.GroupDetail;
import com.recallio.group.domain.GroupExceptions.InvitationNotFoundException;
import com.recallio.group.domain.GroupExceptions.NoActiveInvitationException;
import com.recallio.group.domain.GroupInvitation;
import com.recallio.group.domain.GroupInvitationRepository;
import com.recallio.group.domain.InvitationToken;
import com.recallio.group.domain.StudyGroup;
import com.recallio.group.domain.StudyGroupRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Le lien d'invitation d'un groupe (CLAUDE.md, section 6.4 ; ADR 0016) : le créateur le génère, le remplace ou le
 * désactive ; quiconque l'a reçu et est connecté rejoint le groupe tant qu'il n'a pas expiré.
 */
@ApplicationScoped
public class GroupInvitationService {

    private final GroupInvitationRepository invitations;
    private final StudyGroupRepository groups;
    private final GroupAccess access;
    private final StudyGroupService groupService;
    private final Clock clock;

    public GroupInvitationService(
            GroupInvitationRepository invitations,
            StudyGroupRepository groups,
            GroupAccess access,
            StudyGroupService groupService,
            Clock clock) {
        this.invitations = invitations;
        this.groups = groups;
        this.access = access;
        this.groupService = groupService;
        this.clock = clock;
    }

    /** Génère un nouveau lien ; le précédent cesse aussitôt de fonctionner. */
    @Transactional
    public GroupInvitation renew(UserRef user, UUID groupId) {
        access.ownedGroup(user, groupId);
        GroupInvitation invitation = GroupInvitation.create(groupId, InvitationToken.generate(), clock.instant());
        invitations.replace(invitation);
        return invitation;
    }

    /** @throws NoActiveInvitationException si le groupe n'a pas de lien, ou si le sien a expiré */
    @Transactional(Transactional.TxType.SUPPORTS)
    public GroupInvitation activeInvitation(UserRef user, UUID groupId) {
        access.ownedGroup(user, groupId);
        return invitations
                .findByGroup(groupId)
                .filter(invitation -> invitation.isActiveAt(clock.instant()))
                .orElseThrow(NoActiveInvitationException::new);
    }

    /** Désactive le lien : plus personne ne peut rejoindre le groupe avec lui. */
    @Transactional
    public void revoke(UserRef user, UUID groupId) {
        access.ownedGroup(user, groupId);
        invitations.deleteByGroup(groupId);
    }

    /** Ce que voit l'invité avant d'accepter : de quel groupe il s'agit. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public InvitationPreview preview(UserRef user, String token) {
        GroupInvitation invitation = activeInvitation(token);
        StudyGroup group = existingGroup(invitation);
        return new InvitationPreview(
                group.id(),
                group.name().value(),
                group.members().size(),
                invitation.expiresAt(),
                group.member(user.id()).isPresent());
    }

    /** Rejoint le groupe. Accepter une seconde fois ne change rien : on est déjà membre. */
    @Transactional
    public GroupDetail accept(UserRef user, String token) {
        GroupInvitation invitation = activeInvitation(token);
        StudyGroup group = existingGroup(invitation);
        if (group.member(user.id()).isEmpty()) {
            groups.addMember(group.id(), group.newMember(user.id(), clock.instant()));
        }
        return groupService.group(user, group.id());
    }

    private GroupInvitation activeInvitation(String token) {
        Instant now = clock.instant();
        return InvitationToken.parse(token)
                .flatMap(invitations::findByToken)
                .filter(invitation -> invitation.isActiveAt(now))
                .orElseThrow(InvitationNotFoundException::new);
    }

    /** Le lien disparaît avec son groupe (cascade) : ce cas ne se produit que lors d'une suppression concurrente. */
    private StudyGroup existingGroup(GroupInvitation invitation) {
        return groups.findById(invitation.groupId()).orElseThrow(InvitationNotFoundException::new);
    }

    /** @param alreadyMember vrai si l'invité fait déjà partie du groupe */
    public record InvitationPreview(
            UUID groupId, String groupName, int memberCount, Instant expiresAt, boolean alreadyMember) {}
}
