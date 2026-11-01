package com.recallio.content;

/**
 * Un choix proposé par un QCM ({@link CardType#MULTIPLE_CHOICE}).
 *
 * @param text en Markdown
 * @param correct vrai pour une bonne réponse ; un QCM en a au moins une
 */
public record CardChoice(String text, boolean correct) {

    /** Un QCM propose de 2 à 8 choix. */
    public static final int MIN_PER_CARD = 2;

    public static final int MAX_PER_CARD = 8;
}
