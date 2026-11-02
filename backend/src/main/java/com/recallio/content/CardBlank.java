package com.recallio.content;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Un trou d'une carte « compléter le code » ({@link CardType#FILL_IN}, ADR 0027) et les réponses qu'il accepte.
 *
 * <p>Une réponse est juste si elle est égale à l'une des réponses acceptées, aux espaces près : {@code Objects.hash(x,
 * y)} et {@code Objects.hash(x,y)} se valent. La casse compte, comme dans du code.
 *
 * @param accepted les réponses acceptées, la première étant celle qu'on montre à la correction
 */
public record CardBlank(List<String> accepted) {

    /** Marque un trou dans la question, dans l'ordre des trous. */
    public static final String MARKER = "___";

    public static final int MAX_PER_CARD = 10;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public CardBlank {
        accepted = List.copyOf(accepted);
        if (accepted.isEmpty() || accepted.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Un trou accepte au moins une réponse, et aucune réponse vide");
        }
    }

    /** La réponse montrée à la correction. */
    public String expected() {
        return accepted.getFirst();
    }

    public boolean accepts(String answer) {
        String normalized = normalize(answer);
        return accepted.stream().map(CardBlank::normalize).anyMatch(normalized::equals);
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text).replaceAll("");
    }
}
