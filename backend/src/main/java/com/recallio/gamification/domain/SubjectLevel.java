package com.recallio.gamification.domain;

/**
 * Le niveau d'un sujet d'après son XP (ADR 0035) : passer du niveau n au niveau n + 1 demande {@value #STEP} × n XP. On
 * atteint ainsi le niveau 2 à 100 XP, le 3 à 300, le 5 à 1 000 et le 10 à 4 500 : les premiers niveaux viennent vite,
 * les suivants récompensent la régularité.
 *
 * @param level le niveau, à partir de 1
 * @param xp l'XP du sujet
 * @param levelXp l'XP à laquelle ce niveau commence
 * @param nextLevelXp l'XP à laquelle commence le niveau suivant
 */
public record SubjectLevel(int level, long xp, long levelXp, long nextLevelXp) {

    /** L'XP qui sépare le niveau 1 du niveau 2 ; l'écart grandit d'autant à chaque niveau. */
    public static final int STEP = 100;

    public static SubjectLevel of(long xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("L'XP d'un sujet ne peut pas être négative");
        }
        int level = 1;
        while (start(level + 1) <= xp) {
            level++;
        }
        return new SubjectLevel(level, xp, start(level), start(level + 1));
    }

    /** L'XP à laquelle commence un niveau : {@value #STEP} × (1 + 2 + … + (level − 1)). */
    static long start(int level) {
        return (long) STEP * level * (level - 1) / 2;
    }
}
