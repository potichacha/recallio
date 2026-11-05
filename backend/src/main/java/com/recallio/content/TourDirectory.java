package com.recallio.content;

import java.util.Optional;

/**
 * Les pages du Tour, vues par les autres modules (ADR 0037) : de quoi juger un « À toi » sur le serveur, sans que sa
 * sortie attendue ne quitte le backend.
 */
public interface TourDirectory {

    /**
     * Une page d'une notion du Tour d'un sujet publié.
     *
     * @param page son rang dans la notion, à partir de 1
     * @return vide si la notion est inconnue, retirée, hors du Tour, ou n'a pas cette page
     */
    Optional<TourPage> page(String subjectSlug, String notionSlug, int page);
}
