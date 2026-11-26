package com.recallio.content;

/**
 * Les sujets qu'un appelant peut voir (CLAUDE.md, section 2.5) : tout le monde voit les sujets publiés, un admin voit
 * aussi les brouillons, pour les relire en conditions réelles avant de les publier.
 */
public enum SubjectScope {
    /** Les sujets publiés seulement. */
    PUBLISHED,
    /** Les sujets publiés et les brouillons : pour un admin. */
    WITH_DRAFTS
}
