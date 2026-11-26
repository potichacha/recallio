package com.recallio.entitlement;

import java.util.UUID;

/**
 * API publique du module entitlement (ADR 0001, ADR 0079) : ce que les autres modules savent du droit « Plus ». Ils ne
 * connaissent ni les codes d'accès, ni la date de fin.
 */
public interface PlusAccess {

    /** Vrai si ce compte a Plus maintenant, d'après l'horloge injectée. */
    boolean hasPlus(UUID userId);
}
