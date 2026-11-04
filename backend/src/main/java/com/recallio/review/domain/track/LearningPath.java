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
 */
public final class LearningPath {

    /** Cartes réussies pour acquérir une notion : assez pour avoir vu plusieurs formats, sans attendre les trente. */
    public static final int RECALLED_TO_ACQUIRE = 5;

    private final Map<UUID, NotionInfo> notions;
    private final Map<UUID, Integer> recalled;

    /**
     * @param notions les notions des sujets concernés
     * @param recalledPerNotion le nombre de cartes réussies au moins une fois, par notion
     */
    public LearningPath(Collection<NotionInfo> notions, Map<UUID, Integer> recalledPerNotion) {
        this.notions = notions.stream().collect(Collectors.toMap(NotionInfo::id, Function.identity()));
        this.recalled = Map.copyOf(recalledPerNotion);
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
     * @param prerequisitesApply vrai en parcours « apprendre » : une notion aux prérequis non acquis y est verrouillée
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
        return seen > 0 ? NotionStatus.IN_PROGRESS : NotionStatus.AVAILABLE;
    }

    /** L'état d'une notion pour l'utilisateur, du moins au plus avancé. */
    public enum NotionStatus {
        /** Pas encore de carte : la notion est à rédiger. */
        NO_CARDS,
        /** Un prérequis n'est pas encore acquis (parcours « apprendre »). */
        LOCKED,
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
}
