package com.recallio.user.domain;

/** Nom affiché (classements, groupes) : non vide, sans espaces superflus, de longueur bornée. */
public record DisplayName(String value) {

    public static final int MAX_LENGTH = 50;
    private static final String FALLBACK = "Candidat";

    public DisplayName {
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidProfileException(
                    "user.invalid-display-name",
                    "Le nom affiché doit contenir entre 1 et " + MAX_LENGTH + " caractères.");
        }
    }

    /** Nom proposé par le fournisseur d'identité, ou un nom générique s'il est absent ou inutilisable. */
    public static DisplayName suggestedFrom(String candidate) {
        String stripped = candidate.strip();
        if (stripped.isEmpty()) {
            return new DisplayName(FALLBACK);
        }
        return new DisplayName(stripped.length() > MAX_LENGTH ? stripped.substring(0, MAX_LENGTH) : stripped);
    }
}
