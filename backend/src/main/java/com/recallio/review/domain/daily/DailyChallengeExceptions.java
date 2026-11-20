package com.recallio.review.domain.daily;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus du défi du jour (ADR 0067), chacun avec son code d'erreur stable. */
public final class DailyChallengeExceptions {

    private DailyChallengeExceptions() {}

    public static final class DailyChallengeNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "daily-challenge.not-found";

        public DailyChallengeNotFoundException(UUID id) {
            super(CODE, "Aucun défi du jour " + id + ".");
        }
    }

    /** Le sujet n'a pas assez de notions différentes avec des questions corrigées par le serveur : 422. */
    public static final class DailyChallengeUnavailableException extends DomainException {

        public static final String CODE = "daily-challenge.unavailable";

        public DailyChallengeUnavailableException(String subjectSlug) {
            super(
                    CODE,
                    "Le sujet « %s » n'a pas encore assez de questions pour un défi du jour : essaie un autre sujet."
                            .formatted(subjectSlug));
        }
    }

    /** Une seule réponse par question : la première fait foi. */
    public static final class DailyChallengeAlreadyAnsweredException extends ConflictException {

        public static final String CODE = "daily-challenge.already-answered";

        public DailyChallengeAlreadyAnsweredException(int position) {
            super(
                    CODE,
                    "Tu as déjà répondu à la question %d de ce défi : ta première réponse compte."
                            .formatted(position + 1));
        }
    }

    /**
     * Le défi n'est pas celui du jour du joueur, ni celui de la veille qu'il avait commencé : il ne se joue plus, ou pas
     * encore.
     */
    public static final class DailyChallengeClosedException extends ConflictException {

        public static final String CODE = "daily-challenge.closed";

        public DailyChallengeClosedException() {
            super(CODE, "Ce défi n'est plus ouvert : joue celui d'aujourd'hui.");
        }
    }

    /** Le défi n'a pas de question à cette position, ou sa carte a été retirée depuis le tirage. */
    public static final class NotAChallengeQuestionException extends ResourceNotFoundException {

        public static final String CODE = "daily-challenge.not-a-question";

        public NotAChallengeQuestionException(int position) {
            super(CODE, "Ce défi n'a pas de question %d.".formatted(position + 1));
        }
    }
}
