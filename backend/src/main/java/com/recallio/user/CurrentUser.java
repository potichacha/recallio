package com.recallio.user;

import com.recallio.user.api.TokenIdentity;
import com.recallio.user.application.UserProfileService;
import com.recallio.user.domain.UserProfile;
import jakarta.enterprise.context.RequestScoped;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * API publique du module user pour les autres modules (ADR 0001) : l'utilisateur de la requête en cours. Son profil
 * est créé s'il n'existe pas encore (en UTC ; la PWA appelle {@code GET /api/v1/me} dès la connexion, avec le fuseau du
 * navigateur, donc ce cas ne se produit qu'avec un client qui ne le fait pas).
 */
@RequestScoped
public class CurrentUser {

    private final TokenIdentity identity;
    private final UserProfileService profiles;
    private @Nullable UserRef resolved;

    public CurrentUser(TokenIdentity identity, UserProfileService profiles) {
        this.identity = identity;
        this.profiles = profiles;
    }

    public UserRef get() {
        UserRef current = resolved;
        if (current == null) {
            UserProfile profile = profiles.currentProfile(identity.current(), Optional.empty());
            current = new UserRef(profile.id(), profile.timezone());
            resolved = current;
        }
        return current;
    }
}
