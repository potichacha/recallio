package com.recallio.review.domain;

/**
 * Les quatre notes de FSRS (CLAUDE.md, section 6.1). La valeur numérique est celle de l'algorithme et du journal de
 * révision : ne pas la modifier.
 */
public enum Rating {
    /** « À revoir » : la réponse est oubliée. */
    AGAIN(1),
    /** « Difficile » : retrouvée avec effort. */
    HARD(2),
    /** « Bien » : retrouvée correctement. */
    GOOD(3),
    /** « Facile » : retrouvée sans effort. */
    EASY(4);

    private final int value;

    Rating(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Rating fromValue(int value) {
        for (Rating rating : values()) {
            if (rating.value == value) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Note FSRS inconnue : " + value);
    }
}
