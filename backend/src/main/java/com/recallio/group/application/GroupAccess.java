package com.recallio.group.application;

import com.recallio.group.domain.GroupExceptions.GroupNotFoundException;
import com.recallio.group.domain.GroupExceptions.OwnerOnlyException;
import com.recallio.group.domain.StudyGroup;
import com.recallio.group.domain.StudyGroupRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Le contrôle d'accès commun aux cas d'usage des groupes (CLAUDE.md, section 4.5 ; ADR 0015). */
@ApplicationScoped
public class GroupAccess {

    private final StudyGroupRepository groups;

    public GroupAccess(StudyGroupRepository groups) {
        this.groups = groups;
    }

    /** @throws GroupNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en est pas membre */
    public StudyGroup visibleGroup(UserRef user, UUID groupId) {
        return groups.findById(groupId)
                .filter(group -> group.member(user.id()).isPresent())
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    /**
     * @throws GroupNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en est pas membre
     * @throws OwnerOnlyException s'il en est membre sans en être le créateur
     */
    public StudyGroup ownedGroup(UserRef user, UUID groupId) {
        StudyGroup group = visibleGroup(user, groupId);
        group.requireOwner(user.id());
        return group;
    }
}
