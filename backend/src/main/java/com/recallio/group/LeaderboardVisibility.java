package com.recallio.group;

/** Ce que le classement du groupe montre à ses membres (CLAUDE.md, section 6.4). Stocké tel quel en base. */
public enum LeaderboardVisibility {
    /** Chaque membre voit les noms. */
    NAMED,
    /** Chacun voit sa place et les scores, sans les noms des autres. */
    ANONYMOUS,
    /** Pas de classement. */
    HIDDEN
}
