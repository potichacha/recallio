package com.recallio.review;

import com.recallio.user.UserRef;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement public du module review (ADR 0001, ADR 0012) : une réponse vient d'être enregistrée pour la première fois.
 * Une requête rejouée ne le publie pas. Il est publié dans la transaction de la révision : ses observateurs
 * synchrones (gamification) réussissent ou échouent avec elle.
 *
 * @param reviewId identifiant de l'entrée du journal de révision, unique par réponse
 */
public record CardReviewed(UserRef user, UUID reviewId, UUID cardId, Instant reviewedAt) {}
