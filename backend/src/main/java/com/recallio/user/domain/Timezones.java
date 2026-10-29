package com.recallio.user.domain;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;

/** Lecture des fuseaux horaires : identifiants IANA ({@code Europe/Paris}), {@code UTC} ou décalages fixes. */
public final class Timezones {

    /** Fuseau d'un profil créé sans indication du navigateur. */
    public static final ZoneId DEFAULT = ZoneId.of("UTC");

    private Timezones() {}

    /** Le fuseau correspondant à l'identifiant, s'il est valide ({@code Europe/Paris}, {@code UTC}...). */
    public static Optional<ZoneId> parse(String id) {
        try {
            return Optional.of(ZoneId.of(id.strip()));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    public static ZoneId require(String id) {
        return parse(id)
                .orElseThrow(() -> new InvalidProfileException(
                        "user.invalid-timezone", "Fuseau horaire inconnu : « " + id + " » (exemple : Europe/Paris)."));
    }
}
