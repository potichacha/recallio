package com.recallio.live.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ForbiddenException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus du module live, chacun avec son code d'erreur stable. */
public final class LiveExceptions {

    private LiveExceptions() {}

    /** La partie n'existe pas (ou plus), ou appartient à un groupe dont l'appelant n'est pas membre. */
    public static final class LiveGameNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "live.game-not-found";

        public LiveGameNotFoundException(UUID gameId) {
            super(CODE, "Aucune partie live " + gameId + " dans tes groupes.");
        }
    }

    /** Un groupe a au plus une partie ouverte : on rejoint celle qui existe plutôt que d'en lancer une autre. */
    public static final class LiveGameAlreadyOpenException extends ConflictException {

        public static final String CODE = "live.game-already-open";

        public LiveGameAlreadyOpenException() {
            super(CODE, "Une partie live est déjà ouverte dans ce groupe : rejoins-la.");
        }
    }

    /** L'instance a atteint sa limite de parties simultanées : un refus temporaire. */
    public static final class TooManyLiveGamesException extends ConflictException {

        public static final String CODE = "live.too-many-games";

        public TooManyLiveGamesException() {
            super(CODE, "Trop de parties live sont en cours : réessaie dans quelques minutes.");
        }
    }

    public static final class LiveGameFullException extends ConflictException {

        public static final String CODE = "live.game-full";

        public LiveGameFullException(int limit) {
            super(CODE, "Cette partie compte déjà " + limit + " joueurs, le maximum.");
        }
    }

    public static final class HostOnlyException extends ForbiddenException {

        public static final String CODE = "live.host-only";

        public HostOnlyException() {
            super(CODE, "Seul l'hôte de la partie peut la démarrer.");
        }
    }

    public static final class LiveGameStartedException extends ConflictException {

        public static final String CODE = "live.already-started";

        public LiveGameStartedException() {
            super(CODE, "La partie a déjà démarré.");
        }
    }

    /** On répond depuis la page de la partie, une fois connecté : sinon on ne voit pas la question. */
    public static final class NotInGameException extends ConflictException {

        public static final String CODE = "live.not-in-game";

        public NotInGameException() {
            super(CODE, "Rejoins la partie pour répondre.");
        }
    }

    public static final class RoundClosedException extends ConflictException {

        public static final String CODE = "live.round-closed";

        public RoundClosedException() {
            super(CODE, "Cette question n'attend plus de réponse.");
        }
    }

    public static final class NotEnoughQuestionsException extends DomainException {

        public static final String CODE = "live.not-enough-questions";

        public NotEnoughQuestionsException(int available, int minimum) {
            super(
                    CODE,
                    "Ce sujet ne compte que " + available + " QCM dans les thèmes du groupe ; il en faut au moins "
                            + minimum + " pour une partie.");
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
