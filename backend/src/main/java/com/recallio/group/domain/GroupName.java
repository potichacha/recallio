package com.recallio.group.domain;

/** Nom d'un groupe : non vide, sans espaces superflus, de longueur bornée. */
public record GroupName(String value) {

    public static final int MAX_LENGTH = 60;

    public GroupName {
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidGroupException(
                    "group.invalid-name", "Le nom du groupe doit contenir entre 1 et " + MAX_LENGTH + " caractères.");
        }
    }
}
