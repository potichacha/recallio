package com.recallio.review.domain.drill;

import com.recallio.content.NotionInfo.Level;
import java.util.Set;

/**
 * Le poste visé en révision intensive (ADR 0078) : il choisit les notions dont viennent les questions, d'après leur
 * niveau et leur priorité dans le référentiel (ADR 0025). Le nom sort dans l'API : ne pas renommer une valeur existante.
 */
public enum DrillLevel {
    /** Les notions débutantes et intermédiaires, essentielles et courantes. */
    JUNIOR(Set.of(Level.BEGINNER, Level.INTERMEDIATE), 2),
    /** Les notions intermédiaires et expertes, jusqu'aux avancées. */
    CONFIRMED(Set.of(Level.INTERMEDIATE, Level.EXPERT), 3),
    /** Les notions intermédiaires et expertes, jusqu'aux rares. */
    SENIOR(Set.of(Level.INTERMEDIATE, Level.EXPERT), 4);

    private final Set<Level> notionLevels;
    private final int lastPriority;

    DrillLevel(Set<Level> notionLevels, int lastPriority) {
        this.notionLevels = notionLevels;
        this.lastPriority = lastPriority;
    }

    /**
     * Vrai si une notion de ce niveau et de cette priorité donne des questions à ce poste.
     *
     * @param priority 1 essentiel, 2 courant, 3 avancé, 4 rare
     */
    public boolean accepts(Level notionLevel, int priority) {
        return notionLevels.contains(notionLevel) && priority <= lastPriority;
    }
}
