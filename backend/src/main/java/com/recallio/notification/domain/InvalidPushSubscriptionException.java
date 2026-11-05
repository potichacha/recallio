package com.recallio.notification.domain;

import com.recallio.platform.domain.DomainException;

/** L'adresse n'est pas celle d'un service de notification connu, en https (ADR 0047). */
public class InvalidPushSubscriptionException extends DomainException {

    public static final String CODE = "notification.invalid-subscription";

    public InvalidPushSubscriptionException() {
        super(CODE, "Cet abonnement ne vient pas d'un service de notification connu.");
    }
}
