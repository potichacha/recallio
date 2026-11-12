package com.recallio.review.domain.track;

import com.recallio.content.NotionInfo;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le chemin d'apprentissage d'un sujet (ADR 0032) : quelles notions sont acquises, et donc quelles notions sont
 * débloquées pour le parcours « apprendre ».
 *
 * <p>Une notion est <em>acquise</em> quand {@link #RECALLED_TO_ACQUIRE} de ses cartes ont été réussies (notées au moins
 * « Bien »), ou toutes si elle en a moins ; une notion sans carte est acquise, sinon elle bloquerait tout ce qui en
 * dépend. Une notion est <em>débloquée</em> quand tous ses prérequis sont acquis.
 *
 * <p>Qui apprend découvre une notion dans le Tour avant de la réviser (ADR 0059) : une notion qui a des pages dans le
 * Tour <em>attend le Tour</em> tant qu'elle n'y est pas validée. Une notion sans page n'attend rien.
 */
public final class LearningPath {

    /** Cartes réussies pour acquérir une notion : assez pour avoir vu plusieurs formats, sans attendre les trente. */
    public static final int RECALLED_TO_ACQUIRE = 5;

    private final Map<UUID, NotionInfo> notions;
    private final Map<UUID, Integer> recalled;
    private final Set<UUID> validatedInTour;

    /**
     * @param notions les notions des sujets concernés
     * @param recalledPerNotion le nombre de cartes réussies au moins une fois, par notion
     * @param validatedInTour les notions que l'utilisateur a validées dans le Tour
     */
    public LearningPath(
            Collection<NotionInfo> notions, Map<UUID, Integer> recalledPerNotion, Set<UUID> validatedInTour) {
        this.notions = notions.stream().collect(Collectors.toMap(NotionInfo::id, Function.identity()));
        this.recalled = Map.copyOf(recalledPerNotion);
        this.validatedInTour = Set.copyOf(validatedInTour);
    }

    /** Vrai si la notion a des pages dans le Tour et n'y est pas encore validée. */
    public boolean awaitsTour(UUID notionId) {
        NotionInfo notion = notions.get(notionId);
        return notion != null && notion.isInTour() && !validatedInTour.contains(notionId);
    }

    public boolean acquired(UUID notionId) {
        NotionInfo notion = notions.get(notionId);
        if (notion == null) {
            // Une notion retirée ou inconnue ne bloque rien.
            return true;
        }
        return isAcquired(recalled.getOrDefault(notionId, 0), notion.cardCount());
    }

    /** La règle « acquise », partagée avec la maîtrise (ADR 0034) : assez de cartes réussies, ou toutes. */
    static boolean isAcquired(int recalledCards, int cardCount) {
        return recalledCards >= cardsToAcquire(cardCount);
    }

    static int cardsToAcquire(int cardCount) {
        return Math.min(RECALLED_TO_ACQUIRE, cardCount);
    }

    public boolean unlocked(UUID notionId) {
        NotionInfo notion = notions.get(notionId);
        return notion == null || notion.requires().stream().allMatch(this::acquired);
    }

    /**
     * L'état d'une notion sur la carte du parcours.
     *
     * @param seen les cartes de la notion déjà vues au moins une fois
     * @param prerequisitesApply vrai en parcours « apprendre » : une notion aux prérequis non acquis y est verrouillée,
     *     et une notion du Tour jamais commencée y attend sa validation dans le Tour
     */
    public NotionStatus status(UUID notionId, int seen, boolean prerequisitesApply) {
        NotionInfo notion = notions.get(notionId);
        if (notion == null || notion.cardCount() == 0) {
            return NotionStatus.NO_CARDS;
        }
        if (acquired(notionId)) {
            return NotionStatus.ACQUIRED;
        }
        if (prerequisitesApply && !unlocked(notionId)) {
            return NotionStatus.LOCKED;
        }
        // Une notion déjà commencée continue : la règle ne retire rien à qui révisait avant elle.
        if (prerequisitesApply && seen == 0 && awaitsTour(notionId)) {
            return NotionStatus.TOUR_FIRST;
        }
        return seen > 0 ? NotionStatus.IN_PROGRESS : NotionStatus.AVAILABLE;
    }

    /** L'état d'une notion pour l'utilisateur, du moins au plus avancé. */
    public enum NotionStatus {
        /** Pas encore de carte : la notion est à rédiger. */
        NO_CARDS,
        /** Un prérequis n'est pas encore acquis (parcours « apprendre »). */
        LOCKED,
        /** À découvrir d'abord dans le Tour, où elle n'est pas encore validée (parcours « apprendre »). */
        TOUR_FIRST,
        /** Aucune carte vue. */
        AVAILABLE,
        /** Des cartes vues, pas encore assez de réussies. */
        IN_PROGRESS,
        /** Assez de cartes réussies : la notion débloque celles qui en dépendent. */
        ACQUIRED
    }

    /** Les notions verrouillées : au moins un prérequis n'est pas encore acquis. */
    public Set<UUID> locked() {
        return notions.keySet().stream().filter(notion -> !unlocked(notion)).collect(Collectors.toUnmodifiableSet());
    }

    /** Les notions du Tour qui n'y sont pas encore validées. */
    public Set<UUID> awaitingTour() {
        return notions.keySet().stream().filter(this::awaitsTour).collect(Collectors.toUnmodifiableSet());
    }
}
