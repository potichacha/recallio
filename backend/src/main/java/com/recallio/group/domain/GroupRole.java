package com.recallio.group.domain;

/** Rôle dans un groupe. Stocké tel quel en base. */
public enum GroupRole {
    /** Le créateur : gère les membres et les réglages, supprime le groupe. */
    OWNER,
    MEMBER
}
