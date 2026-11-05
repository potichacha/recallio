package com.recallio.review.domain.exam;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** Les refus de l'examen blanc (ADR 0044), chacun avec son code d'erreur stable. */
public final class MockExamExceptions {

    private MockExamExceptions() {}

    /** L'examen n'existe pas, ou appartient à un autre utilisateur : les deux cas répondent de la même façon. */
    public static final class MockExamNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "review.mock-exam-not-found";

        public MockExamNotFoundException(UUID id) {
            super(CODE, "Aucun examen blanc " + id + " parmi les vôtres.");
        }
    }

    public static final class MockExamAlreadySubmittedException extends ConflictException {

        public static final String CODE = "review.mock-exam-already-submitted";

        public MockExamAlreadySubmittedException(UUID id) {
            super(CODE, "La copie de l'examen " + id + " est déjà rendue.");
        }
    }

    public static final class NotEnoughExamQuestionsException extends DomainException {

        public static final String CODE = "review.not-enough-exam-questions";

        public NotEnoughExamQuestionsException(String subjectSlug, int minimum) {
            super(
                    CODE,
                    "Le sujet « %s » n'a pas encore %d questions corrigées par le serveur pour un examen blanc."
                            .formatted(subjectSlug, minimum));
        }
    }
}
