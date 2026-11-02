package com.recallio.review.domain;

import com.recallio.platform.domain.DomainException;

/**
 * La réponse ne correspond pas au format de la carte : un QCM attend les choix cochés et un code à trous ses réponses (le
 * serveur en déduit la note), les autres formats une note d'auto-évaluation.
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

    public static AnswerMismatchException ratingExpected() {
        return new AnswerMismatchException("Cette carte se note par auto-évaluation, sans choix cochés.");
    }
}
