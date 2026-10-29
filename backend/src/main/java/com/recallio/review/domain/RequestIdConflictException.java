package com.recallio.review.domain;

import com.recallio.platform.domain.ConflictException;
import java.util.UUID;

/** L'identifiant de requête a déjà servi pour une autre révision (autre carte, autre note ou autre utilisateur). */
public class RequestIdConflictException extends ConflictException {

    public static final String CODE = "review.request-id-conflict";

    public RequestIdConflictException(UUID requestId) {
        super(CODE, "L'identifiant de requête " + requestId + " a déjà été utilisé pour une autre révision.");
    }
}
