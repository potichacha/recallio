package com.recallio.user.domain;

import java.util.UUID;

/**
 * Les réglages du profil public d'un utilisateur (ADR 0043).
 *
 * @param visible faux tant que l'utilisateur ne l'a pas rendu public ; il peut le rendre privé à tout moment
 */
public record PublicProfileSettings(UUID userId, PublicHandle handle, boolean visible) {}
