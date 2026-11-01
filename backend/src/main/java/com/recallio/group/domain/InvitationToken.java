package com.recallio.group.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Le secret d'un lien d'invitation : 24 octets aléatoires (192 bits), encodés en base64url sans remplissage, soit 32
 * caractères. Impossible à deviner, il suffit à prouver qu'on a reçu le lien.
 */
public record InvitationToken(String value) {

    private static final int RANDOM_BYTES = 24;
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{32}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public InvitationToken {
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Jeton d'invitation mal formé");
        }
    }

    public static InvitationToken generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return new InvitationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /** Le jeton lu dans une URL, ou vide s'il n'a pas le bon format (il ne correspond alors à aucune invitation). */
    public static Optional<InvitationToken> parse(String value) {
        return FORMAT.matcher(value).matches() ? Optional.of(new InvitationToken(value)) : Optional.empty();
    }

    /** Le jeton n'apparaît jamais en clair dans un message d'erreur ou un log. */
    @Override
    public String toString() {
        return "InvitationToken[***]";
    }
}
