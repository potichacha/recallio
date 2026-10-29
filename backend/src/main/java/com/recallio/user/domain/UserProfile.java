package com.recallio.user.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Profil applicatif d'un utilisateur. L'identité (mot de passe, e-mail) reste chez le fournisseur OIDC : on ne garde
 * que ce dont le produit a besoin.
 *
 * @param oidcSubject le claim {@code sub} du jeton, stable chez le fournisseur
 * @param timezone fuseau de l'utilisateur, qui définit « le jour » d'un streak
 */
public record UserProfile(UUID id, String oidcSubject, DisplayName displayName, ZoneId timezone, Instant createdAt) {

    public UserProfile withPreferences(DisplayName newDisplayName, ZoneId newTimezone) {
        return new UserProfile(id, oidcSubject, newDisplayName, newTimezone, createdAt);
    }
}
