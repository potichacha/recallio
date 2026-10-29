package com.recallio.platform.domain;

import java.util.UUID;

/** Fournit les identifiants des entités métier (UUID v7, voir ADR 0008). Injecté pour garder les tests déterministes. */
public interface IdGenerator {

    UUID newId();
}
