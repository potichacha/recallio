package com.recallio.content.domain.source;

/**
 * Lit une source de contenu et produit thèmes et cartes. Port du domaine : la source actuelle est un fichier Markdown
 * unique, mais elle pourra devenir un fichier par carte sans toucher à l'import (CLAUDE.md, section 5.2).
 */
public interface ContentParser {

    ParsedContent parse(String source);
}
