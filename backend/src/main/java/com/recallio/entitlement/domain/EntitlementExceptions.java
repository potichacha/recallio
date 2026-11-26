package com.recallio.entitlement.domain;

import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.platform.domain.TooManyRequestsException;
import java.util.UUID;

/** Les refus du module entitlement, chacun avec son code d'erreur stable. */
public final class EntitlementExceptions {

    private EntitlementExceptions() {}

    /**
     * Le code ne marche pas : inconnu, mal formé, épuisé, expiré ou révoqué. Une seule erreur pour tous ces cas : elle
     * ne dit pas si le code a existé.
     */
    public static final class InvalidAccessCodeException extends DomainException {

        public static final String CODE = "entitlement.access-code-invalid";

        public InvalidAccessCodeException() {
            super(CODE, "Ce code ne marche pas, ou plus. Vérifie que tu l'as recopié en entier.");
        }
    }

    /** Ce compte a déjà utilisé ce code : lui seul l'apprend, et il le savait. */
    public static final class AccessCodeAlreadyRedeemedException extends ConflictException {

        public static final String CODE = "entitlement.access-code-already-redeemed";

        public AccessCodeAlreadyRedeemedException() {
            super(CODE, "Tu as déjà utilisé ce code : un code sert une seule fois par compte.");
        }
    }

    public static final class TooManyCodeAttemptsException extends TooManyRequestsException {

        public static final String CODE = "entitlement.too-many-code-attempts";

        public TooManyCodeAttemptsException() {
            super(CODE, "Tu as essayé trop de codes qui ne marchent pas. Réessaie dans une heure.");
        }
    }

    public static final class BatchNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "entitlement.batch-not-found";

        public BatchNotFoundException(UUID id) {
            super(CODE, "Aucun lot de codes " + id + ".");
        }
    }

    /** Un lot dont les codes seraient expirés dès leur création. */
    public static final class BatchAlreadyExpiredException extends DomainException {

        public static final String CODE = "entitlement.batch-already-expired";

        public BatchAlreadyExpiredException() {
            super(CODE, "La date d'expiration des codes est déjà passée.");
        }
    }
}
