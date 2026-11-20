package com.recallio.review.domain.interview;

import com.recallio.platform.domain.DomainException;

/** Les refus du compte à rebours avant un entretien (ADR 0066), chacun avec son code d'erreur stable. */
public final class InterviewExceptions {

    private InterviewExceptions() {}

    /** La date n'est pas après aujourd'hui, dans le fuseau de l'utilisateur, ou elle est trop loin. */
    public static final class InvalidInterviewDateException extends DomainException {

        public static final String CODE = "interview.invalid-date";

        public InvalidInterviewDateException(int maxDaysAhead) {
            super(CODE, "Choisis une date après aujourd'hui, dans les %d prochains jours.".formatted(maxDaysAhead));
        }
    }

    /** Une date ne se donne qu'à un sujet suivi, comme le parcours. */
    public static final class InterviewSubjectNotFollowedException extends DomainException {

        public static final String CODE = "interview.subject-not-followed";

        public InterviewSubjectNotFollowedException(String subjectSlug) {
            super(
                    CODE,
                    "Tu ne suis pas le sujet « %s » : suis-le d'abord pour y prévoir ton entretien."
                            .formatted(subjectSlug));
        }
    }

    /** Le plan répartit les notions essentielles : un sujet qui n'en a pas (sans référentiel) n'en a pas. */
    public static final class NoEssentialNotionsException extends DomainException {

        public static final String CODE = "interview.no-essential-notions";

        public NoEssentialNotionsException(String subjectSlug) {
            super(
                    CODE,
                    "Le sujet « %s » n'a pas encore de notions essentielles : il n'a pas de plan avant un entretien."
                            .formatted(subjectSlug));
        }
    }
}
