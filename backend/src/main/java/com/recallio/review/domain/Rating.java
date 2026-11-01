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

    /**
     * La note d'une réponse corrigée automatiquement (QCM) : juste, la carte est « retrouvée » ; fausse, elle est à
     * revoir. Ni « Difficile » ni « Facile » : le serveur ne sait pas avec quel effort la réponse a été trouvée.
     */
    public static Rating ofGradedAnswer(boolean correct) {
        return correct ? GOOD : AGAIN;
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
