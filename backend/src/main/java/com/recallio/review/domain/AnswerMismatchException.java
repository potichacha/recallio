package com.recallio.review.domain;

import com.recallio.platform.domain.DomainException;
import java.util.Collection;

/**
 * La réponse ne correspond pas au format de la carte : un QCM attend les choix cochés, un code à trous ses réponses et un
 * exercice ses fichiers (le serveur en déduit la note), les autres formats une note d'auto-évaluation.
 */
public class AnswerMismatchException extends DomainException {

    public static final String CODE = "review.answer-mismatch";

    private AnswerMismatchException(String detail) {
        super(CODE, detail);
    }

    public static AnswerMismatchException choicesExpected() {
        return new AnswerMismatchException("Un QCM se répond en cochant des choix, sans note : le serveur corrige.");
    }

    public static AnswerMismatchException blanksExpected(int blankCount) {
        return new AnswerMismatchException(
                "Un code à trous se répond en remplissant ses %d trou(s), sans note : le serveur corrige."
                        .formatted(blankCount));
    }

    public static AnswerMismatchException filesExpected() {
        return new AnswerMismatchException(
                "Un exercice de code se répond en rendant ses fichiers, sans note : ses tests le jugent.");
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
                "Cette carte s'auto-évalue : seuls un vrai ou faux, un QCM et un code à trous se corrigent.");
    }

    public static AnswerMismatchException ratingExpected() {
        return new AnswerMismatchException("Cette carte se note par auto-évaluation, sans choix cochés.");
    }
}
