package com.recallio.review;

import com.recallio.user.UserRef;
import java.time.Instant;

/**
 * Événement public du module review (ADR 0037, ADR 0040) : un utilisateur connecté vient d'ouvrir une page du Tour. Il
 * est publié dans la transaction qui note la dernière page ; les quêtes en comptent les pages lues.
 *
 * @param page le rang de la page dans sa notion, à partir de 1
 */
public record TourPageOpened(UserRef user, String subjectSlug, String notionSlug, int page, Instant openedAt) {}
