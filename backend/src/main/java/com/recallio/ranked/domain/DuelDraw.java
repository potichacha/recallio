package com.recallio.ranked.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Le tirage des questions d'un duel (ADR 0046) : parmi les cartes dont la cote de difficulté est la plus proche de
 * celle du joueur, au hasard, une par notion tant que possible. Le hasard vient d'une graine, pour les tests.
 */
public final class DuelDraw {

    /** Les cartes les plus proches du niveau, parmi lesquelles le hasard choisit. */
    static final int POOL = 25;

    private DuelDraw() {}

    /**
     * Une question possible.
     *
     * @param notionId sa notion ; {@code null} pour une carte d'un document Markdown, qui compte comme sa propre notion
     * @param rating sa cote de difficulté ; celle de départ si personne n'y a encore répondu
     */
    public record Candidate(UUID cardId, @Nullable UUID notionId, double rating) {}

    public static List<UUID> draw(List<Candidate> candidates, double target, int count, long seed) {
        List<Candidate> pool = new ArrayList<>(candidates);
        pool.sort(Comparator.comparingDouble(candidate -> Math.abs(candidate.rating() - target)));
        pool = new ArrayList<>(pool.subList(0, Math.min(pool.size(), Math.max(POOL, count))));
        Collections.shuffle(pool, new Random(seed));
        List<UUID> drawn = new ArrayList<>();
        Set<UUID> notions = new HashSet<>();
        // Un premier passage prend une carte par notion ; le second complète si les notions manquent.
        for (Candidate candidate : pool) {
            UUID notionId = candidate.notionId();
            UUID notion = notionId != null ? notionId : candidate.cardId();
            if (drawn.size() < count && notions.add(notion)) {
                drawn.add(candidate.cardId());
            }
        }
        for (Candidate candidate : pool) {
            if (drawn.size() < count && !drawn.contains(candidate.cardId())) {
                drawn.add(candidate.cardId());
            }
        }
        return List.copyOf(drawn);
    }
}
