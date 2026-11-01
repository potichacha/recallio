package com.recallio.group.application;

import com.recallio.group.domain.GroupExceptions.GroupNotFoundException;
import com.recallio.group.domain.GroupExceptions.MemberNotFoundException;
import com.recallio.group.domain.GroupExceptions.TooManyGroupsException;
import com.recallio.group.domain.GroupName;
import com.recallio.group.domain.GroupRole;
import com.recallio.group.domain.StudyGroup;
import com.recallio.group.domain.StudyGroupRepository;
import com.recallio.group.domain.StudyGroupRepository.GroupSummary;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cas d'usage des groupes (CLAUDE.md, section 6.4). L'autorisation est vérifiée ici, pour chaque groupe : seul un membre
 * voit un groupe, seul le créateur en gère les membres (CLAUDE.md, section 4.5).
 */
@ApplicationScoped
public class StudyGroupService {

    /** Évite qu'un compte crée des groupes à l'infini. */
    static final int MAX_OWNED_GROUPS = 20;

    /** Un membre dont le profil a disparu (compte supprimé) reste listé, sans nom. */
    private static final String UNKNOWN_MEMBER = "Membre";

    private final StudyGroupRepository groups;
    private final UserDirectory users;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public StudyGroupService(StudyGroupRepository groups, UserDirectory users, IdGenerator idGenerator, Clock clock) {
        this.groups = groups;
        this.users = users;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /** Crée un groupe dont l'utilisateur est le créateur, et son seul membre. */
    @Transactional
    public GroupDetail create(UserRef user, String name) {
        GroupName groupName = new GroupName(name);
        if (groups.countOwnedBy(user.id()) >= MAX_OWNED_GROUPS) {
            throw new TooManyGroupsException(MAX_OWNED_GROUPS);
        }
        StudyGroup group = StudyGroup.create(idGenerator.newId(), groupName, user.id(), clock.instant());
        groups.create(group);
        return detail(group, user);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GroupSummary> myGroups(UserRef user) {
        return groups.findByMember(user.id());
    }

    /** @throws GroupNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en est pas membre */
    @Transactional(Transactional.TxType.SUPPORTS)
    public GroupDetail group(UserRef user, UUID groupId) {
        return detail(visibleGroup(user, groupId), user);
    }

    /** Quitter un groupe. Le créateur ne le peut pas : il supprime le groupe. */
    @Transactional
    public void leave(UserRef user, UUID groupId) {
        StudyGroup group = visibleGroup(user, groupId);
        group.requireRemovable(user.id());
        groups.removeMember(groupId, user.id());
    }

    /** Le créateur retire un membre. */
    @Transactional
    public void removeMember(UserRef user, UUID groupId, UUID memberId) {
        StudyGroup group = visibleGroup(user, groupId);
        group.requireOwner(user.id());
        if (group.member(memberId).isEmpty()) {
            throw new MemberNotFoundException(memberId);
        }
        group.requireRemovable(memberId);
        groups.removeMember(groupId, memberId);
    }

    /** Le créateur supprime le groupe et toutes ses adhésions. */
    @Transactional
    public void delete(UserRef user, UUID groupId) {
        visibleGroup(user, groupId).requireOwner(user.id());
        groups.delete(groupId);
    }

    private StudyGroup visibleGroup(UserRef user, UUID groupId) {
        return groups.findById(groupId)
                .filter(group -> group.member(user.id()).isPresent())
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    private GroupDetail detail(StudyGroup group, UserRef viewer) {
        Map<UUID, String> names = users.displayNames(
                group.members().stream().map(StudyGroup.Member::userId).toList());
        List<MemberView> members = group.members().stream()
                .map(member -> new MemberView(
                        member.userId(),
                        names.getOrDefault(member.userId(), UNKNOWN_MEMBER),
                        member.role(),
                        member.joinedAt(),
                        member.userId().equals(viewer.id())))
                .toList();
        GroupRole myRole = group.isOwner(viewer.id()) ? GroupRole.OWNER : GroupRole.MEMBER;
        return new GroupDetail(group.id(), group.name().value(), group.createdAt(), myRole, members);
    }

    public record GroupDetail(UUID id, String name, Instant createdAt, GroupRole myRole, List<MemberView> members) {}

    /** @param me vrai pour l'utilisateur qui consulte le groupe */
    public record MemberView(UUID userId, String displayName, GroupRole role, Instant joinedAt, boolean me) {}
}
