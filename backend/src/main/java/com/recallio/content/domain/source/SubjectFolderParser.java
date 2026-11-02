package com.recallio.content.domain.source;

/**
 * Lit le dossier d'un sujet (son en-tête, son référentiel de notions et les cartes de chaque notion) et produit thèmes,
 * notions et cartes (ADR 0026). Second format de source à côté du document Markdown ({@link ContentParser}) : chaque
 * sujet a l'un ou l'autre.
 */
public interface SubjectFolderParser {

    /** @throws InvalidSourceException si l'en-tête ou le référentiel est illisible : le sujet ne peut pas être importé */
    ParsedContent parse(SubjectFolder folder);
}
