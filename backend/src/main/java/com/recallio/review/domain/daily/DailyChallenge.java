package com.recallio.review.domain.daily;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Le défi du jour d'un sujet (ADR 0067) : cinq questions, les mêmes pour tous ceux qui le jouent ce jour-là, chacun à son
 * jour. Il est tiré à la première demande, puis gardé : une correction du contenu dans la journée ne le change pas.
 *
 * @param day le jour du défi : celui de chaque joueur, dans son fuseau
 * @param cardIds les questions, dans l'ordre où elles sont posées ; leur position est leur rang dans cette liste
 */
public record DailyChallenge(UUID id, String subjectSlug, LocalDate day, List<UUID> cardIds, Instant createdAt) {

    /** Le nombre de questions d'un défi. */
    public static final int QUESTIONS = 5;

    /** Les cartes des derniers défis d'un sujet ne reviennent pas : un joueur régulier ne revoit pas une question. */
    public static final int RECENT_CHALLENGES = 30;

    public DailyChallenge {
        cardIds = List.copyOf(cardIds);
        if (cardIds.isEmpty()) {
            throw new IllegalArgumentException("Un défi a au moins une question");
        }
    }
}
