package com.recallio.content;

import java.util.List;

/** API publique du module content : les sujets que les autres modules peuvent référencer (ADR 0001). */
public interface SubjectDirectory {

    /** Les slugs des sujets publiés, dans l'ordre du catalogue. Un brouillon n'y figure pas. */
    List<String> publishedSubjectSlugs();

    /** @throws com.recallio.platform.domain.ResourceNotFoundException si aucun sujet publié ne porte ce slug */
    void requirePublished(String slug);
}
