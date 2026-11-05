package com.recallio.review;

import com.recallio.user.UserRef;
import java.time.Instant;

/**
 * Événement public du module review (ADR 0037, ADR 0040) : un utilisateur vient de valider une notion du Tour pour la
 * première fois. Valider de nouveau une notion déjà validée ne le publie pas.
 *
 * @param validatedNotions toutes les notions que l'utilisateur a validées, tous sujets confondus, celle-ci comprise
 */
public record TourNotionValidated(
        UserRef user, String subjectSlug, String notionSlug, long validatedNotions, Instant validatedAt) {}
