package com.recallio.live.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus du module live, chacun avec son code d'erreur stable. */
public final class LiveExceptions {

    private LiveExceptions() {}

    /** La partie n'existe pas (ou plus), ou appartient à un groupe dont l'appelant n'est pas membre. */
    public static final class LiveGameNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "live.game-not-found";

        public LiveGameNotFoundException(UUID gameId) {
            super(CODE, "Aucune partie live " + gameId + " dans vos groupes.");
        }
    }

    /** Un groupe a au plus une partie ouverte : on rejoint celle qui existe plutôt que d'en lancer une autre. */
    public static final class LiveGameAlreadyOpenException extends ConflictException {

        public static final String CODE = "live.game-already-open";

        public LiveGameAlreadyOpenException() {
            super(CODE, "Une partie live est déjà ouverte dans ce groupe : rejoignez-la.");
        }
    }

    public static final class LiveGameFullException extends ConflictException {

        public static final String CODE = "live.game-full";

        public LiveGameFullException(int limit) {
            super(CODE, "Cette partie compte déjà " + limit + " joueurs, le maximum.");
        }
    }

    /** Une partie porte sur l'un des sujets choisis pour le groupe (ADR 0017). */
    public static final class SubjectNotInGroupException extends DomainException {

        public static final String CODE = "live.subject-not-in-group";

        public SubjectNotInGroupException(String subjectSlug) {
            super(CODE, "Le sujet « " + subjectSlug + " » ne fait pas partie des sujets du groupe.");
        }
    }
}
