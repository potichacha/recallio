package com.recallio.ranked.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus du classé (ADR 0046), chacun avec son code d'erreur stable. */
public final class RankedExceptions {

    private RankedExceptions() {}

    /** Le duel n'existe pas, ou l'utilisateur n'y joue pas : les deux cas répondent de la même façon. */
    public static final class DuelNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "ranked.duel-not-found";

        public DuelNotFoundException(UUID id) {
            super(CODE, "Aucun duel " + id + " parmi les vôtres.");
        }
    }

    public static final class NotEnoughDuelQuestionsException extends DomainException {

        public static final String CODE = "ranked.not-enough-questions";

        public NotEnoughDuelQuestionsException(String subjectSlug) {
            super(
                    CODE,
                    "Le sujet « %s » n'a pas encore %d questions corrigées par le serveur pour un duel."
                            .formatted(subjectSlug, Duel.QUESTIONS));
        }
    }

    /** Le duel est fini ou expiré, ou le joueur a déjà répondu à toutes ses questions. */
    public static final class DuelOverException extends ConflictException {

        public static final String CODE = "ranked.duel-over";

        public DuelOverException(UUID id) {
            super(CODE, "Le duel " + id + " n'attend plus de réponse de votre part.");
        }
    }

    /** La question à laquelle on répond n'est pas celle qui a été servie, ou a déjà sa réponse. */
    public static final class QuestionNotServedException extends ConflictException {

        public static final String CODE = "ranked.question-not-served";

        public QuestionNotServedException(int position) {
            super(
                    CODE,
                    "La question %d n'attend pas de réponse : demandez la question suivante.".formatted(position + 1));
        }
    }

    public static final class AnswerShapeException extends DomainException {

        public static final String CODE = "ranked.answer-mismatch";

        public AnswerShapeException() {
            super(CODE, "Cette réponse ne convient pas au format de la question : des choix, ou un mot par trou.");
        }
    }
}
