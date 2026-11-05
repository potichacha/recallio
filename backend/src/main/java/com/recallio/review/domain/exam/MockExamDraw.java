package com.recallio.review.domain.exam;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Le tirage des questions d'un examen blanc (ADR 0044), comme un recruteur les choisirait : les notions essentielles
 * d'abord, une question par notion tant qu'il en reste, au hasard parmi celles de même priorité. Le hasard vient d'une
 * graine, pour que le tirage soit reproductible en test.
 */
public final class MockExamDraw {

    private MockExamDraw() {}

    /**
     * Une question possible.
     *
     * @param notionId sa notion ; {@code null} pour une carte d'un document Markdown, qui compte comme sa propre notion
     * @param priority celle de sa notion, de 1 (essentiel) à 4 (rare)
     */
    public record Candidate(UUID cardId, @Nullable UUID notionId, int priority) {}

    public static List<UUID> draw(List<Candidate> candidates, int count, long seed) {
        List<Candidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new Random(seed));
        // Le tri est stable : à priorité égale, l'ordre du mélange reste.
        shuffled.sort(Comparator.comparingInt(Candidate::priority));
        Map<UUID, Deque<Candidate>> byNotion = new LinkedHashMap<>();
        for (Candidate candidate : shuffled) {
            UUID notionId = candidate.notionId();
            UUID notion = notionId != null ? notionId : candidate.cardId();
            byNotion.computeIfAbsent(notion, _ -> new ArrayDeque<>()).add(candidate);
        }
        List<UUID> drawn = new ArrayList<>();
        // Un tour prend une question de chaque notion, dans l'ordre des priorités ; le suivant, une deuxième.
        while (drawn.size() < count && !byNotion.isEmpty()) {
            Iterator<Deque<Candidate>> notions = byNotion.values().iterator();
            while (notions.hasNext() && drawn.size() < count) {
                Deque<Candidate> questions = notions.next();
                drawn.add(questions.removeFirst().cardId());
                if (questions.isEmpty()) {
                    notions.remove();
                }
            }
        }
        return List.copyOf(drawn);
    }
}
