package com.recallio.content.domain.source;

/**
 * Lit le document d'un sujet et produit thèmes et cartes. Port du domaine : la source actuelle est un fichier Markdown
 * par sujet, mais elle pourra devenir un fichier par carte sans toucher à l'import (CLAUDE.md, section 5.2).
 */
public interface ContentParser {

    /** @throws InvalidSourceException si le document ne dit pas de quel sujet il s'agit (en-tête absent ou invalide) */
    ParsedContent parse(String source);
}
