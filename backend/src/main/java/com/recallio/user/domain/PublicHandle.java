package com.recallio.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * L'adresse d'un profil public (ADR 0043), {@code /u/<handle>} : de 3 à 30 caractères, minuscules, chiffres et tirets,
 * sans tiret au début ni à la fin. La saisie est mise en minuscules.
 */
public record PublicHandle(String value) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;
    public static final String CODE = "user.invalid-handle";

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]*[a-z0-9]");

    public PublicHandle {
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() < MIN_LENGTH
                || value.length() > MAX_LENGTH
                || !PATTERN.matcher(value).matches()) {
            throw new InvalidProfileException(
                    CODE,
                    "L'adresse du profil fait de %d à %d caractères : minuscules, chiffres et tirets, sans tiret au début ni à la fin."
                            .formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }
}
