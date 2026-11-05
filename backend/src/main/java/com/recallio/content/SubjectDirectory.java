package com.recallio.content;

import java.util.List;
import java.util.Map;

/** API publique du module content : les sujets que les autres modules peuvent référencer (ADR 0001). */
public interface SubjectDirectory {

    /** Les slugs des sujets publiés, dans l'ordre du catalogue. Un brouillon n'y figure pas. */
    List<String> publishedSubjectSlugs();

    /** Le titre de chaque sujet publié, par slug, dans l'ordre du catalogue. */
    Map<String, String> publishedTitles();

    /** @throws com.recallio.platform.domain.ResourceNotFoundException si aucun sujet publié ne porte ce slug */
    void requirePublished(String slug);

    /**
     * Les slugs des thèmes d'un sujet publié, dans l'ordre du programme.
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si aucun sujet publié ne porte ce slug
     */
    List<String> themeSlugs(String subjectSlug);
}
