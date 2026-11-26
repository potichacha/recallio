package com.recallio.review.domain.drill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Le tirage des questions de la révision intensive (ADR 0078), sans rien garder sur le serveur : la graine du client
 * fixe l'ordre de toutes les cartes éligibles, et {@code offset} dit où il en est dans cet ordre.
 *
 * <p>Un <em>tour</em> pose chaque carte une fois. Il prend une carte de chaque notion, puis une deuxième, et ainsi de
 * suite : deux cartes d'une même notion sont séparées par autant de questions qu'il reste de notions. Le tour fini, le
 * suivant repart dans un autre ordre, tiré de la même graine : la révision n'a pas de fin.
 */
public final class DrillDraw {

    private DrillDraw() {}

    /** Une carte éligible et sa notion. */
    public record Candidate(UUID cardId, UUID notionId) {}

    /**
     * @param cardIds les cartes de la page, dans l'ordre où les poser, une par notion au plus
     * @param total le nombre de cartes éligibles : la longueur d'un tour
     * @param nextOffset où reprendre pour la page suivante
     */
    public record Page(List<UUID> cardIds, int total, long nextOffset) {

        public Page {
            cardIds = List.copyOf(cardIds);
        }
    }

    /**
     * La page qui commence à {@code offset}. Elle s'arrête à {@code limit} cartes, à la fin du tour, ou juste avant une
     * carte dont la notion est déjà sur la page : elle peut donc être plus courte que {@code limit}, et la suivante
     * reprend à {@link Page#nextOffset()}, sans sauter de carte. L'ordre des candidats ne compte pas.
     *
     * @param offset le nombre de cartes déjà servies depuis le début, tous tours confondus ; positif ou nul
     * @param limit le nombre de cartes voulu ; au moins 1
     */
    public static Page page(List<Candidate> candidates, long seed, long offset, int limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("offset %d ou limit %d hors bornes".formatted(offset, limit));
        }
        int total = candidates.size();
        if (total == 0) {
            return new Page(List.of(), 0, 0);
        }
        List<Candidate> lap = lap(candidates, seed, offset / total);
        int start = (int) (offset % total);
        List<UUID> cardIds = new ArrayList<>();
        Set<UUID> notions = new HashSet<>();
        for (int index = start; index < total && cardIds.size() < limit; index++) {
            Candidate candidate = lap.get(index);
            if (!notions.add(candidate.notionId())) {
                break;
            }
            cardIds.add(candidate.cardId());
        }
        return new Page(cardIds, total, offset + cardIds.size());
    }

    /** L'ordre de toutes les cartes pour ce tour : mélangées, puis une carte de chaque notion à tour de rôle. */
    private static List<Candidate> lap(List<Candidate> candidates, long seed, long lap) {
        List<Candidate> shuffled = new ArrayList<>(candidates);
        // Le tri rend le mélange indépendant de l'ordre dans lequel le catalogue donne les cartes.
        shuffled.sort(Comparator.comparing(Candidate::cardId));
        Collections.shuffle(shuffled, new Random(Objects.hash(seed, lap)));
        Map<UUID, Deque<Candidate>> byNotion = new LinkedHashMap<>();
        for (Candidate candidate : shuffled) {
            byNotion.computeIfAbsent(candidate.notionId(), _ -> new ArrayDeque<>())
                    .add(candidate);
        }
        List<Candidate> ordered = new ArrayList<>(shuffled.size());
        while (!byNotion.isEmpty()) {
            Iterator<Deque<Candidate>> notions = byNotion.values().iterator();
            while (notions.hasNext()) {
                Deque<Candidate> cards = notions.next();
                ordered.add(cards.removeFirst());
                if (cards.isEmpty()) {
                    notions.remove();
                }
            }
        }
        return ordered;
    }
}
