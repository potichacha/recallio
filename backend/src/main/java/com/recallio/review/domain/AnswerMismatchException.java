package com.recallio.review.domain;

import com.recallio.platform.domain.DomainException;
import java.util.Collection;

/**
 * La réponse ne correspond pas à la carte : une carte à choix attend les choix cochés, un code à trous ses réponses et un
 * exercice ses fichiers. Le serveur en déduit la note ; une carte qu'il ne sait pas corriger ne se révise pas (ADR
 * 0063).
 */
public class AnswerMismatchException extends DomainException {

    public static final String CODE = "review.answer-mismatch";

    private AnswerMismatchException(String detail) {
        super(CODE, detail);
    }

    public static AnswerMismatchException choicesExpected() {
        return new AnswerMismatchException("Cette carte se répond en cochant ses choix : le serveur corrige.");
    }

    public static AnswerMismatchException blanksExpected(int blankCount) {
        return new AnswerMismatchException(
                "Un code à trous se répond en remplissant ses %d trou(s) : le serveur corrige.".formatted(blankCount));
    }

    public static AnswerMismatchException filesExpected() {
        return new AnswerMismatchException(
                "Un exercice de code se répond en rendant ses fichiers : ses tests le jugent.");
    }

    public static AnswerMismatchException starterFilesExpected(Collection<String> paths) {
        return new AnswerMismatchException(
                "Un exercice se rend avec exactement les fichiers de son code de départ : " + String.join(", ", paths));
    }

    public static AnswerMismatchException notAnExercise() {
        return new AnswerMismatchException("Seul un exercice de code s'exécute.");
    }

    public static AnswerMismatchException tourAnswersExpected() {
        return new AnswerMismatchException(
                "Une notion du Tour se valide en répondant à chacune de ses questions, une fois chacune.");
    }

    public static AnswerMismatchException notGradedByServer() {
        return new AnswerMismatchException(
                "Cette carte n'a pas encore ses choix : le serveur ne peut pas la corriger, elle ne se révise pas.");
    }
}
