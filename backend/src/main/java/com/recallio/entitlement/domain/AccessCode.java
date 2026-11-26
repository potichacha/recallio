package com.recallio.entitlement.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Un code d'accès (ADR 0079) : un secret de 25 caractères tirés parmi 32, soit 125 bits, écrit en cinq groupes
 * ({@code K7QW2-M9XDF-4TLPA-8RBNC-H3VYE}). Son alphabet n'a ni 0, ni O, ni 1, ni I : il se lit et se dicte sans
 * hésiter.
 *
 * <p>Le code n'est gardé nulle part : la base ne connaît que son {@linkplain #fingerprint() empreinte} et ses
 * {@linkplain #suffix() derniers caractères}.
 *
 * @param symbols les 25 caractères, en majuscules, sans tirets
 */
public record AccessCode(String symbols) {

    /** 32 caractères : chacun porte 5 bits, et un tirage uniforme n'a pas de biais. */
    static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    static final int GROUPS = 5;
    static final int GROUP_LENGTH = 5;
    static final int LENGTH = GROUPS * GROUP_LENGTH;

    /** Ce qu'on garde en clair pour reconnaître un code dans une liste : 20 bits sur 125. */
    public static final int SUFFIX_LENGTH = 4;

    /** Bien plus qu'un code écrit avec ses tirets et quelques espaces : au-delà, la saisie n'est pas lue. */
    public static final int MAX_INPUT_LENGTH = 64;

    private static final char SEPARATOR = '-';
    private static final Pattern FORMAT = Pattern.compile("[" + ALPHABET + "]{" + LENGTH + "}");

    /** Les espaces et les tirets de toute sorte ne comptent pas. */
    private static final Pattern IGNORED = Pattern.compile("[\\s\\p{Pd}]");

    public AccessCode {
        if (!FORMAT.matcher(symbols).matches()) {
            throw new IllegalArgumentException("Code d'accès mal formé");
        }
    }

    /** Un nouveau code, tiré par un générateur cryptographique. */
    public static AccessCode generate(SecureRandom random) {
        StringBuilder symbols = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            symbols.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new AccessCode(symbols.toString());
    }

    /**
     * Le code saisi, sans tenir compte de la casse, des espaces ni des tirets ; vide s'il n'a pas la forme d'un code (il
     * ne correspond alors à aucun).
     */
    public static Optional<AccessCode> parse(String input) {
        if (input.length() > MAX_INPUT_LENGTH) {
            return Optional.empty();
        }
        String symbols = IGNORED.matcher(input).replaceAll("").toUpperCase(Locale.ROOT);
        return FORMAT.matcher(symbols).matches() ? Optional.of(new AccessCode(symbols)) : Optional.empty();
    }

    /** Le code tel qu'on le donne à lire : en groupes séparés par des tirets. */
    public String formatted() {
        StringBuilder formatted = new StringBuilder(LENGTH + GROUPS - 1);
        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                formatted.append(SEPARATOR);
            }
            formatted.append(symbols, group * GROUP_LENGTH, (group + 1) * GROUP_LENGTH);
        }
        return formatted.toString();
    }

    /**
     * L'empreinte SHA-256 du code, en hexadécimal : la seule chose que la base en garde. Sans sel ni fonction lente : le
     * code est un secret aléatoire de 125 bits, pas un mot de passe, et rien ne permet de l'énumérer.
     */
    public String fingerprint() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(symbols.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est garanti par la plateforme Java", e);
        }
    }

    /** Les derniers caractères, gardés en clair. */
    public String suffix() {
        return symbols.substring(LENGTH - SUFFIX_LENGTH);
    }

    /** Le code n'apparaît jamais en clair dans un message d'erreur ou un log. */
    @Override
    public String toString() {
        return "AccessCode[***]";
    }
}
