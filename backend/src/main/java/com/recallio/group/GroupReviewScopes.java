package com.recallio.group;

import com.recallio.content.CardType;
import com.recallio.user.UserRef;
import java.util.Set;
import java.util.UUID;

/** API publique du module group (ADR 0001) : ce qu'un groupe fait réviser à ses membres, pour le module review. */
public interface GroupReviewScopes {

    /**
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le groupe n'existe pas ou si l'utilisateur n'en
     *     est pas membre
     */
    GroupReviewScope of(UserRef user, UUID groupId);

    /**
     * @param subjectSlugs les sujets du groupe ; aucun : rien à réviser
     * @param themeSlugs restreint chaque sujet aux thèmes listés qui lui appartiennent ; aucun : tous les thèmes
     * @param cardTypes les formats retenus ; aucun : tous les formats
     */
    record GroupReviewScope(Set<String> subjectSlugs, Set<String> themeSlugs, Set<CardType> cardTypes) {

        public GroupReviewScope {
            subjectSlugs = Set.copyOf(subjectSlugs);
            themeSlugs = Set.copyOf(themeSlugs);
            cardTypes = Set.copyOf(cardTypes);
        }
    }
}
