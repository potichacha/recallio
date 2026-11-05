package com.recallio.gamification.domain.quest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** La progression des quêtes de chaque utilisateur (ADR 0040). */
public interface QuestRepository {

    /**
     * Ajoute à la progression d'une quête, de façon atomique : deux révisions simultanées comptent deux fois.
     *
     * @return la progression, cet ajout compris
     */
    int advance(UUID userId, Quest quest, LocalDate periodStart, int amount);

    /**
     * Termine la quête, si elle ne l'est pas déjà.
     *
     * @return vrai si c'est cet appel qui l'a terminée : son XP ne s'attribue qu'une fois
     */
    boolean complete(UUID userId, Quest quest, LocalDate periodStart, Instant completedAt);

    /**
     * Note qu'un élément a été vu dans la période : un format de carte, un jour actif, une page du Tour.
     *
     * @return vrai si c'est la première fois
     */
    boolean firstTime(UUID userId, LocalDate periodStart, String item);

    /** La progression de quêtes commencées dans ces périodes ; une quête qui n'a pas commencé est absente. */
    Map<QuestKey, QuestState> states(UUID userId, Collection<LocalDate> periodStarts);

    record QuestKey(Quest quest, LocalDate periodStart) {}

    /** @param completedAt absent tant que la quête n'est pas terminée */
    record QuestState(int progress, @Nullable Instant completedAt) {}
}
