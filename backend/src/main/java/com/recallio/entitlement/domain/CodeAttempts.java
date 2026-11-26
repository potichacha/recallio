package com.recallio.entitlement.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Les essais ratés d'un compte, contre l'essai en force (ADR 0079) : une fenêtre d'une heure, qui s'ouvre au premier
 * essai raté, et le nombre d'essais ratés depuis.
 */
public record CodeAttempts(Instant windowStartedAt, int failures) {

    /** Au-delà, le compte attend la fin de la fenêtre : de quoi se tromper en recopiant, pas de quoi deviner. */
    public static final int MAX_FAILURES = 10;

    public static final Duration WINDOW = Duration.ofHours(1);

    /** Vrai si le compte a épuisé ses essais et que sa fenêtre n'est pas finie. */
    public boolean isBlockedAt(Instant now) {
        return failures >= MAX_FAILURES && !isOverAt(now);
    }

    /** Vrai si la fenêtre est finie : le prochain essai raté en ouvre une autre. */
    public boolean isOverAt(Instant now) {
        return !now.isBefore(windowStartedAt.plus(WINDOW));
    }

    /** Les essais après un essai raté de plus : dans la même fenêtre, ou dans une nouvelle si elle est finie. */
    public CodeAttempts afterFailureAt(Instant now) {
        return isOverAt(now) ? first(now) : new CodeAttempts(windowStartedAt, failures + 1);
    }

    /** Le premier essai raté d'un compte : il ouvre sa fenêtre. */
    public static CodeAttempts first(Instant now) {
        return new CodeAttempts(now, 1);
    }
}
