package com.recallio.group.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.ForbiddenException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus du module group, chacun avec son code d'erreur stable. */
public final class GroupExceptions {

    private GroupExceptions() {}

    /**
     * Le groupe n'existe pas, ou l'appelant n'en est pas membre : les deux cas répondent de la même façon, pour ne pas
     * révéler l'existence d'un groupe à qui n'y appartient pas.
     */
    public static final class GroupNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "group.not-found";

        public GroupNotFoundException(UUID groupId) {
            super(CODE, "Aucun groupe " + groupId + " parmi vos groupes.");
        }
    }

    public static final class MemberNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "group.member-not-found";

        public MemberNotFoundException(UUID userId) {
            super(CODE, "L'utilisateur " + userId + " n'est pas membre de ce groupe.");
        }
    }

    public static final class OwnerOnlyException extends ForbiddenException {

        public static final String CODE = "group.owner-only";

        public OwnerOnlyException() {
            super(CODE, "Seul le créateur du groupe peut faire cette action.");
        }
    }

    /** Un groupe a toujours un créateur : il peut supprimer le groupe, pas le quitter. */
    public static final class OwnerCannotLeaveException extends ConflictException {

        public static final String CODE = "group.owner-cannot-leave";

        public OwnerCannotLeaveException() {
            super(CODE, "Le créateur ne peut pas quitter son groupe : il peut le supprimer.");
        }
    }

    public static final class TooManyGroupsException extends ConflictException {

        public static final String CODE = "group.too-many-groups";

        public TooManyGroupsException(int limit) {
            super(CODE, "Vous avez déjà créé " + limit + " groupes, le maximum.");
        }
    }
}
