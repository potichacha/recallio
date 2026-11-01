package com.recallio.group;

import com.recallio.user.UserRef;
import java.util.List;
import java.util.UUID;

/**
 * API publique du module group (ADR 0001) : ce qu'il faut savoir d'un groupe pour établir son classement, que le module
 * gamification calcule à partir des points et de l'activité qu'il détient (ADR 0018).
 */
public interface GroupLeaderboards {

    /**
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en
     *     est pas membre
     */
    LeaderboardSetup setup(UserRef viewer, UUID groupId);

    /**
     * @param members tous les membres, le créateur compris
     * @param dailyGoal cartes à réviser dans la journée pour qu'elle compte comme régulière
     */
    record LeaderboardSetup(String groupName, List<Member> members, int dailyGoal, LeaderboardVisibility visibility) {

        public LeaderboardSetup {
            members = List.copyOf(members);
        }
    }

    record Member(UUID userId, String displayName) {}
}
