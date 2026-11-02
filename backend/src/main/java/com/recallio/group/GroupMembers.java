package com.recallio.group;

import com.recallio.user.UserRef;
import java.util.UUID;

/** API publique du module group (ADR 0001) : l'appartenance à un groupe, pour les modules qui s'y rattachent (live). */
public interface GroupMembers {

    /**
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en
     *     est pas membre
     */
    void requireMember(UserRef user, UUID groupId);
}
