package com.recallio.content;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Un choix proposé par une carte qui se corrige sur ses choix : un QCM ({@link CardType#MULTIPLE_CHOICE}), un vrai ou
 * faux, ou une carte d'un autre format qui en a reçu (ADR 0051).
 *
 * @param text en Markdown
 * @param correct vrai pour une bonne réponse ; une carte en a au moins une
 */
public record CardChoice(String text, boolean correct) {

    /** Une carte propose de 2 à 8 choix. */
    public static final int MIN_PER_CARD = 2;

    public static final int MAX_PER_CARD = 8;

    /**
     * Les choix d'une carte, vérifiés : de 2 à 8, dont au moins un correct, et deux choix au même texte n'en font pas
     * deux.
     *
     * @throws IllegalArgumentException sinon
     */
    public static List<CardChoice> checked(List<CardChoice> choices) {
        List<CardChoice> copy = List.copyOf(choices);
        if (copy.size() < MIN_PER_CARD
                || copy.size() > MAX_PER_CARD
                || copy.stream().noneMatch(CardChoice::correct)) {
            throw new IllegalArgumentException("Une carte à choix en a de %d à %d, dont au moins un correct"
                    .formatted(MIN_PER_CARD, MAX_PER_CARD));
        }
        Set<String> texts = new HashSet<>();
        for (CardChoice choice : copy) {
            if (!texts.add(choice.text().strip())) {
                throw new IllegalArgumentException("Le choix « " + choice.text().strip() + " » est en double");
            }
        }
        return copy;
    }

    /** Aucun choix, pour une carte qui s'auto-évalue, ou des choix vérifiés comme par {@link #checked}. */
    public static List<CardChoice> checkedIfAny(List<CardChoice> choices) {
        return choices.isEmpty() ? List.of() : checked(choices);
    }
}
