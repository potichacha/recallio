package com.recallio.user.api;

import com.recallio.user.domain.AuthenticatedUser;
import jakarta.enterprise.context.RequestScoped;
import java.util.Objects;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jspecify.annotations.Nullable;

/** Traduit le jeton de la requête en {@link AuthenticatedUser} : seul endroit qui connaît les noms des claims. */
@RequestScoped
public class TokenIdentity {

    private final JsonWebToken token;

    public TokenIdentity(JsonWebToken token) {
        this.token = token;
    }

    public AuthenticatedUser current() {
        String subject = Objects.requireNonNull(token.getSubject(), "Le jeton ne contient pas de claim sub");
        return new AuthenticatedUser(
                subject, firstNonBlank(token.getClaim("name"), token.getClaim(Claims.preferred_username)));
    }

    private static String firstNonBlank(@Nullable Object first, @Nullable Object second) {
        for (Object value : new @Nullable Object[] {first, second}) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }
}
