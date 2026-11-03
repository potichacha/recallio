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
        return recalled.getOrDefault(notionId, 0) >= Math.min(RECALLED_TO_ACQUIRE, notion.cardCount());
    }

    public boolean unlocked(UUID notionId) {
        NotionInfo notion = notions.get(notionId);
        return notion == null || notion.requires().stream().allMatch(this::acquired);
    }

    /** Les notions verrouillées : au moins un prérequis n'est pas encore acquis. */
    public Set<UUID> locked() {
        return notions.keySet().stream().filter(notion -> !unlocked(notion)).collect(Collectors.toUnmodifiableSet());
    }
}
