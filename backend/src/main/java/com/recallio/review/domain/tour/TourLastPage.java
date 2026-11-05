package com.recallio.review.domain.tour;

import java.util.UUID;

/**
 * La dernière page du Tour qu'un utilisateur a ouverte dans un sujet (ADR 0037).
 *
 * @param page son rang dans la notion, à partir de 1
 */
public record TourLastPage(UUID notionId, int page) {}
