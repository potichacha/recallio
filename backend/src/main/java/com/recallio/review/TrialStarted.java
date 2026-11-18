package com.recallio.review;

import java.time.Instant;

/**
 * Événement public du module review (ADR 0050, ADR 0064) : un visiteur vient de commencer l'essai sans compte d'un
 * sujet, en répondant à sa première question. Il ne dit rien du visiteur : l'essai ne le connaît pas.
 *
 * <p>Il n'est pas publié au chargement des questions : la page publique les charge pour chaque visiteur, afin de poser
 * la première dans la page. Il est publié hors de toute transaction, l'essai n'enregistrant rien.
 */
public record TrialStarted(String subjectSlug, Instant startedAt) {}
