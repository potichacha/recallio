package com.recallio.review;

import java.util.List;
import java.util.UUID;

/**
 * API publique du module review (ADR 0001) : les compétences d'un utilisateur, pour son profil public (ADR 0043). Le
 * vérifié (ce que le serveur a corrigé) y est séparé du déclaré (la maîtrise, auto-évaluée en partie).
 */
public interface SkillProfiles {

    /** Les compétences de l'utilisateur dans chacun de ces sujets, dans le même ordre. */
    List<SubjectSkills> of(UUID userId, List<String> subjectSlugs);

    record SubjectSkills(String subjectSlug, Verified verified, Declared declared) {}

    /**
     * Ce que le serveur a vérifié.
     *
     * @param exercisesPassed les exercices de code réussis, jugés par leurs tests
     * @param correctAnswers les autres cartes corrigées par le serveur (QCM, vrai ou faux, code à compléter) réussies
     * @param tourNotionsValidated les notions du Tour validées
     */
    record Verified(int exercisesPassed, int correctAnswers, int tourNotionsValidated) {}

    /**
     * Ce que l'utilisateur déclare en révision personnelle : la maîtrise des notions (ADR 0034), dont une partie
     * s'auto-évalue.
     *
     * @param notions les notions du sujet ; aucune pour un sujet sans référentiel
     * @param readinessPercent son indice de préparation (ADR 0041)
     */
    record Declared(int notions, int acquired, int mastered, int expert, int readinessPercent) {}
}
