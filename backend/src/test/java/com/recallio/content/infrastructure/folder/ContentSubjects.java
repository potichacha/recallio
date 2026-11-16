package com.recallio.content.infrastructure.folder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Les sujets dont {@link CodeOutputTest} exécute les cartes, donnés par la propriété {@code recallio.subjects} : des
 * slugs séparés par des virgules ({@code cpp,go}). Sans elle, tous les sujets le sont.
 *
 * <p>La CI la renseigne pour une PR qui ne touche que du contenu : seuls les sujets modifiés sont rejoués, et une PR de
 * contenu Java ne recompile pas les cartes C++ (CLAUDE.md, section 9, étape 8.10.2). Elle vaut {@code none} quand la PR
 * ne touche aucun dossier de sujet.
 */
final class ContentSubjects {

    private static final Set<String> CHOSEN = Arrays.stream(
                    System.getProperty("recallio.subjects", "").split(","))
            .map(String::strip)
            .filter(subject -> !subject.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

    private ContentSubjects() {}

    /** Vrai si la carte, nommée {@code <sujet>/<fichier>#<clé>} par {@link CodeOutputCards}, est d'un sujet choisi. */
    static boolean includesCard(String name) {
        int end = name.indexOf('/');
        return CHOSEN.isEmpty() || CHOSEN.contains(end < 0 ? name : name.substring(0, end));
    }
}
