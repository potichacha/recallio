package com.recallio.content;

import java.util.List;
import java.util.UUID;

/**
 * Une notion du référentiel d'un sujet (ADR 0025), vue par les autres modules : ce qu'il faut pour ordonner les
 * nouvelles cartes selon un parcours, débloquer les notions (ADR 0032), et les présenter par domaine et chapitre.
 *
 * @param slug l'identifiant de la notion dans son sujet
 * @param domain l'identifiant de son domaine dans le référentiel
 * @param domainTitle le titre de ce domaine
 * @param chapter l'identifiant de son chapitre dans le référentiel
 * @param priority 1 essentiel, 2 courant, 3 avancé, 4 rare
 * @param requires les notions prérequises du même sujet
 * @param position l'ordre de la notion dans le référentiel du sujet
 * @param cardCount les cartes actives de la notion
 */
public record NotionInfo(
        UUID id,
        String subjectSlug,
        String slug,
        String title,
        String domain,
        String domainTitle,
        String chapter,
        String chapterTitle,
        Level level,
        int priority,
        List<UUID> requires,
        int position,
        int cardCount) {

    public NotionInfo {
        requires = List.copyOf(requires);
    }

    /** Le public visé par la notion, du plus débutant au plus expert. */
    public enum Level {
        BEGINNER,
        INTERMEDIATE,
        EXPERT
    }
}
