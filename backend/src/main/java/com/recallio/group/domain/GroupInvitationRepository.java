package com.recallio.group.domain;

import java.util.Optional;
import java.util.UUID;

public interface GroupInvitationRepository {

    /** Enregistre le lien du groupe, en remplaçant le précédent s'il y en avait un. */
    void replace(GroupInvitation invitation);

    /** Le lien du groupe, même expiré. */
    Optional<GroupInvitation> findByGroup(UUID groupId);

    /** Le lien portant ce jeton, même expiré. */
    Optional<GroupInvitation> findByToken(InvitationToken token);

    void deleteByGroup(UUID groupId);
}
