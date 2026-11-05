package com.recallio.review.domain.track;

import java.util.Collection;

/**
 * L'indice de préparation d'un sujet (ADR 0041), comme le « prêt pour l'examen » d'Ornikar : la part de ses notions
 * essentielles (priorité 1) qu'on sait. Une notion acquise compte pour moitié, une notion maîtrisée ou experte en entier
 * : on n'atteint 100 % qu'en maîtrisant toutes les notions essentielles.
 *
 * @param essentials les notions essentielles du sujet
 * @param acquired celles au moins acquises, maîtrisées comprises
 * @param mastered celles au moins maîtrisées
 */
public record Readiness(int essentials, int acquired, int mastered) {

    /** La priorité des notions essentielles, dans le référentiel. */
    public static final int ESSENTIAL_PRIORITY = 1;

    /** À partir de cet indice, en pourcentage, le sujet est prêt pour un entretien. */
    public static final int READY_PERCENT = 80;

    public static final Readiness NONE = new Readiness(0, 0, 0);

    private static final int PERCENT = 100;

    /** @param essentials la maîtrise des notions essentielles du sujet */
    public static Readiness of(Collection<NotionMastery> essentials) {
        MasteryCounts counts = MasteryCounts.of(essentials);
        return new Readiness(counts.total(), counts.acquired(), counts.mastered());
    }

    /** L'indice, en pourcentage arrondi à l'inférieur ; 0 pour un sujet sans notion essentielle. */
    public int percent() {
        if (essentials == 0) {
            return 0;
        }
        // Acquise = un demi-point, maîtrisée = un point ; « acquired » compte déjà les notions maîtrisées.
        return (int) ((acquired + mastered) * (long) PERCENT / (2L * essentials));
    }

    public boolean ready() {
        return essentials > 0 && percent() >= READY_PERCENT;
    }
}
