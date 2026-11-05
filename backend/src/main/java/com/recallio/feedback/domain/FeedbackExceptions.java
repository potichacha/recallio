package com.recallio.feedback.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.platform.domain.TooManyRequestsException;
import java.util.UUID;

/** Les refus du module feedback, chacun avec son code d'erreur stable. */
public final class FeedbackExceptions {

    private FeedbackExceptions() {}

    /** Ni une carte, ni une page du Tour : il faut l'un ou l'autre, pas les deux. */
    public static final class InvalidReportTargetException extends DomainException {

        public static final String CODE = "feedback.invalid-target";

        public InvalidReportTargetException() {
            super(CODE, "Signalez une carte (cardId), ou une page du Tour (subject, notion et page).");
        }
    }

    /** La carte ou la page n'existe pas, a été retirée, ou appartient à un sujet en brouillon. */
    public static final class ReportTargetNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "feedback.target-not-found";

        public ReportTargetNotFoundException() {
            super(CODE, "Ce contenu n'existe pas, ou plus.");
        }
    }

    public static final class ReportAlreadyOpenException extends ConflictException {

        public static final String CODE = "feedback.report-already-open";

        public ReportAlreadyOpenException() {
            super(CODE, "Vous avez déjà signalé ce contenu : votre signalement attend d'être traité.");
        }
    }

    public static final class TooManyOpenReportsException extends TooManyRequestsException {

        public static final String CODE = "feedback.too-many-open-reports";

        public TooManyOpenReportsException(int limit) {
            super(
                    CODE,
                    "Vous avez déjà %d signalements en attente : merci de patienter qu'ils soient traités."
                            .formatted(limit));
        }
    }

    public static final class ReportNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "feedback.report-not-found";

        public ReportNotFoundException(UUID id) {
            super(CODE, "Aucun signalement " + id + ".");
        }
    }

    public static final class ReportAlreadyResolvedException extends ConflictException {

        public static final String CODE = "feedback.report-already-resolved";

        public ReportAlreadyResolvedException(UUID id) {
            super(CODE, "Le signalement " + id + " a déjà été traité.");
        }
    }
}
