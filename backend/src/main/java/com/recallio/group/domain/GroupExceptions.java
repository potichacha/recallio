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

    /**
     * Le lien d'invitation est inconnu, expiré ou remplacé : les trois cas répondent de la même façon, et la réponse ne
     * dit rien du groupe.
     */
    public static final class InvitationNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "group.invitation-not-found";

        public InvitationNotFoundException() {
            super(CODE, "Ce lien d'invitation n'est plus valide : il a expiré ou a été remplacé.");
        }
    }

    /** Le groupe n'a pas de lien d'invitation actif (jamais créé, désactivé ou expiré). */
    public static final class NoActiveInvitationException extends ResourceNotFoundException {

        public static final String CODE = "group.no-active-invitation";

        public NoActiveInvitationException() {
            super(CODE, "Ce groupe n'a pas de lien d'invitation actif.");
        }
    }

    public static final class GroupFullException extends ConflictException {

        public static final String CODE = "group.full";

        public GroupFullException(int limit) {
            super(CODE, "Ce groupe compte déjà " + limit + " membres, le maximum.");
        }
    }

    public static final class TooManyGroupsException extends ConflictException {

        public static final String CODE = "group.too-many-groups";

        public TooManyGroupsException(int limit) {
            super(CODE, "Vous avez déjà créé " + limit + " groupes, le maximum.");
        }
    }
}
