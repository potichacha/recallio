package com.recallio.group.domain;

import com.recallio.group.domain.GroupExceptions.GroupFullException;
import com.recallio.group.domain.GroupExceptions.OwnerCannotLeaveException;
import com.recallio.group.domain.GroupExceptions.OwnerOnlyException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Un groupe de révision (CLAUDE.md, section 6.4) et ses membres. Il a exactement un créateur ({@link GroupRole#OWNER}),
 * qui seul gère les membres et peut supprimer le groupe.
 *
 * @param members dans l'ordre d'arrivée, le créateur en premier
 */
public record StudyGroup(UUID id, GroupName name, Instant createdAt, List<Member> members) {

    /** Un groupe ne compte pas plus de membres que cela (borne la taille des classements et des pages). */
    public static final int MAX_MEMBERS = 100;

    public StudyGroup {
        members = List.copyOf(members);
        if (members.stream().filter(Member::isOwner).count() != 1) {
            throw new IllegalArgumentException("Un groupe a exactement un créateur : " + id);
        }
    }

    public static StudyGroup create(UUID id, GroupName name, UUID ownerId, Instant now) {
        return new StudyGroup(id, name, now, List.of(new Member(ownerId, GroupRole.OWNER, now)));
    }

    /**
     * L'adhésion d'un nouveau membre, qui a reçu le lien d'invitation.
     *
     * @throws GroupFullException si le groupe compte déjà {@link #MAX_MEMBERS} membres
     * @throws IllegalArgumentException s'il en est déjà membre
     */
    public Member newMember(UUID userId, Instant now) {
        if (member(userId).isPresent()) {
            throw new IllegalArgumentException("Déjà membre du groupe " + id);
        }
        if (members.size() >= MAX_MEMBERS) {
            throw new GroupFullException(MAX_MEMBERS);
        }
        return new Member(userId, GroupRole.MEMBER, now);
    }

    public Optional<Member> member(UUID userId) {
        return members.stream().filter(member -> member.userId().equals(userId)).findFirst();
    }

    public boolean isOwner(UUID userId) {
        return member(userId).map(Member::isOwner).orElse(false);
    }

    /** @throws OwnerOnlyException si {@code userId} n'est pas le créateur */
    public void requireOwner(UUID userId) {
        if (!isOwner(userId)) {
            throw new OwnerOnlyException();
        }
    }

    /**
     * Vérifie que {@code userId} peut quitter le groupe, ou en être retiré.
     *
     * @throws OwnerCannotLeaveException si c'est le créateur
     */
    public void requireRemovable(UUID userId) {
        if (isOwner(userId)) {
            throw new OwnerCannotLeaveException();
        }
    }

    public record Member(UUID userId, GroupRole role, Instant joinedAt) {

        public boolean isOwner() {
            return role == GroupRole.OWNER;
        }
    }
}
