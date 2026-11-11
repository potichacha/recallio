package com.recallio.platform.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les données personnelles qu'un module garde sur un utilisateur (ADR 0056). Chaque module qui en garde implémente ce
 * port : le compte s'exporte et se supprime en les appelant tous, sans que le module {@code user} connaisse leurs
 * tables. Une nouvelle table qui porte un utilisateur doit y figurer : {@code AccountDeletionTest} le vérifie.
 */
public interface PersonalData {

    /** Le nom affiché à la place de celui d'un compte supprimé, là où ses traces restent (duels, défis, parties). */
    String DELETED_ACCOUNT_NAME = "Compte supprimé";

    /** Le nom de la section du module dans l'export. */
    String section();

    /** Les lignes de l'utilisateur, par table, telles qu'elles sont enregistrées. */
    Map<String, List<Map<String, @Nullable Object>>> export(UUID userId);

    /**
     * Efface les données de l'utilisateur. Ce que d'autres ont en commun avec lui (un duel, un défi, une partie) reste,
     * rattaché à un identifiant qui ne mène plus à personne.
     */
    void erase(UUID userId);
}
