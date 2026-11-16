package com.recallio.runner.sandbox;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Les sujets dont {@link ExercisesTest} et {@link TourTest} jouent le contenu, donnés par la propriété
 * {@code recallio.subjects} : des slugs séparés par des virgules ({@code cpp,go}). Sans elle, tous les sujets le sont.
 *
 * <p>La CI la renseigne pour une PR qui ne touche que du contenu : seuls les sujets modifiés sont joués dans le bac à
 * sable (CLAUDE.md, section 9, étape 8.10.2). Elle vaut {@code none} quand la PR ne touche aucun dossier de sujet.
 */
final class ContentSubjects {

    private static final Set<String> CHOSEN = Arrays.stream(
                    System.getProperty("recallio.subjects", "").split(","))
            .map(String::strip)
            .filter(subject -> !subject.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

    private ContentSubjects() {}

    /** Vrai si tous les sujets sont joués : aucun n'a été choisi. */
    static boolean all() {
        return CHOSEN.isEmpty();
    }

    /** Vrai si ce chemin, sous {@code content}, est dans le dossier d'un sujet choisi ({@code content/<sujet>/...}). */
    static boolean includes(Path content, Path path) {
        return all() || CHOSEN.contains(content.relativize(path).getName(0).toString());
    }
}
