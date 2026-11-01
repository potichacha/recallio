package com.recallio.group.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyGroupRepository {

    /** Enregistre un nouveau groupe avec son créateur. */
    void create(StudyGroup group);

    /** Le groupe et ses membres. */
    Optional<StudyGroup> findById(UUID groupId);

    /** Les groupes dont l'utilisateur est membre, du plus récemment rejoint au plus ancien. */
    List<GroupSummary> findByMember(UUID userId);

    long countOwnedBy(UUID userId);

    void removeMember(UUID groupId, UUID userId);

    /** Supprime le groupe et toutes ses adhésions. */
    void delete(UUID groupId);

    /** Un groupe vu par l'un de ses membres, pour la liste « mes groupes ». */
    record GroupSummary(UUID id, GroupName name, GroupRole role, int memberCount) {}
}
