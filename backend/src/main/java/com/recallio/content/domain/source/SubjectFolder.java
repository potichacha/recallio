package com.recallio.content.domain.source;

import java.util.Map;

/**
 * Les fichiers du dossier d'un sujet ({@code content/<sujet>/}, ADR 0026), déjà lus : la lecture du disque reste hors
 * du domaine, et le parseur se teste avec des fichiers en mémoire.
 *
 * @param subject le contenu de {@code subject.yaml}
 * @param referential les fichiers de {@code referentiel/}, par nom de fichier ({@code 06-collections.yaml})
 * @param notions les fichiers de {@code notions/}, par chemin relatif à ce dossier ({@code collections/hashset.yaml})
 * @param exercises tous les fichiers de {@code exercices/}, par chemin relatif à ce dossier
 *     ({@code collections/choisir-collection/panier/starter/Panier.java}, ADR 0029)
 */
public record SubjectFolder(
        String subject, Map<String, String> referential, Map<String, String> notions, Map<String, String> exercises) {

    public SubjectFolder {
        referential = Map.copyOf(referential);
        notions = Map.copyOf(notions);
        exercises = Map.copyOf(exercises);
    }

    /** Un dossier sans exercices. */
    public SubjectFolder(String subject, Map<String, String> referential, Map<String, String> notions) {
        this(subject, referential, notions, Map.of());
    }
}
