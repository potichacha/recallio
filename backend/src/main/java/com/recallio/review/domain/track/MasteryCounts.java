package com.recallio.review.domain.track;

import java.util.Collection;

/**
 * Les notions d'un sujet ou d'un domaine par niveau de maîtrise (ADR 0034). Chaque compte inclut les niveaux supérieurs :
 * une notion experte compte aussi comme maîtrisée, acquise et découverte.
 *
 * @param total toutes les notions, nouvelles comprises
 * @param discovered les notions au moins découvertes
 * @param acquired les notions au moins acquises
 * @param mastered les notions au moins maîtrisées
 * @param expert les notions expertes
 */
public record MasteryCounts(int total, int discovered, int acquired, int mastered, int expert) {

    public static final MasteryCounts NONE = new MasteryCounts(0, 0, 0, 0, 0);

    public static MasteryCounts of(Collection<NotionMastery> notions) {
        return new MasteryCounts(
                notions.size(),
                atLeast(notions, MasteryLevel.DISCOVERED),
                atLeast(notions, MasteryLevel.ACQUIRED),
                atLeast(notions, MasteryLevel.MASTERED),
                atLeast(notions, MasteryLevel.EXPERT));
    }

    private static int atLeast(Collection<NotionMastery> notions, MasteryLevel level) {
        return (int) notions.stream()
                .filter(notion -> notion.level().compareTo(level) >= 0)
                .count();
    }
}
