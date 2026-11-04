package com.recallio.gamification.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Registre de l'XP, append-only (CLAUDE.md, section 6.3 ; ADR 0035) : un total est la somme des entrées, jamais un
 * compteur modifié en place. Il permet de recalculer n'importe quel total et d'expliquer chaque entrée.
 */
public interface XpLedger {

    /**
     * Ajoute l'entrée, sauf si une entrée existe déjà pour le même utilisateur, la même raison et la même source.
     */
    void append(XpEntry entry);

    XpTotals totals(UUID userId);

    /**
     * @param subjectSlug le sujet de la carte révisée ; absent pour le bonus de l'objectif du jour, qui n'appartient à
     *     aucun sujet
     * @param sourceId ce qui a rapporté l'XP : la révision, ou celle qui a permis d'atteindre l'objectif
     */
    record XpEntry(
            UUID id,
            UUID userId,
            int amount,
            XpReason reason,
            @Nullable String subjectSlug,
            UUID sourceId,
            Instant occurredAt) {}

    /**
     * @param total toute l'XP de l'utilisateur, bonus de l'objectif du jour compris
     * @param bySubject l'XP de chaque sujet, par slug ; un sujet sans XP est absent
     */
    record XpTotals(long total, Map<String, Long> bySubject) {

        public XpTotals {
            bySubject = Map.copyOf(bySubject);
        }

        public long of(String subjectSlug) {
            return bySubject.getOrDefault(subjectSlug, 0L);
        }
    }
}
