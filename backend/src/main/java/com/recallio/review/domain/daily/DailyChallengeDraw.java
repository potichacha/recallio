package com.recallio.review.domain.daily;

import com.recallio.content.NotionInfo.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Le tirage des questions d'un défi du jour (ADR 0067), de difficulté croissante : deux notions débutantes, deux
 * intermédiaires, une experte. Un niveau qui manque se complète par le plus proche, et l'ordre reste croissant. Les
 * notions sont toutes différentes, de domaines différents autant que possible, et les cartes des derniers défis du sujet
 * ne reviennent pas. Le hasard vient d'une graine, pour que le tirage soit reproductible en test.
 */
public final class DailyChallengeDraw {

    /** Le niveau de chaque question, dans l'ordre où elles sont posées. */
    static final List<Level> LEVELS =
            List.of(Level.BEGINNER, Level.BEGINNER, Level.INTERMEDIATE, Level.INTERMEDIATE, Level.EXPERT);

    private DailyChallengeDraw() {}

    /**
     * Une question possible : une carte que le serveur corrige sans exécuter de code.
     *
     * @param notionId sa notion ; {@code null} pour une carte d'un document Markdown, qui compte comme sa propre notion
     * @param domain le domaine de sa notion dans le référentiel ; {@code null} s'il n'est pas connu
     * @param level le niveau de sa notion ; intermédiaire pour une carte sans notion
     */
    public record Candidate(
            UUID cardId, @Nullable UUID notionId, @Nullable String domain, Level level) {

        UUID notionKey() {
            UUID notion = notionId;
            return notion != null ? notion : cardId;
        }
    }

    /** Vrai s'il y a assez de notions différentes pour un défi. */
    public static boolean canDraw(Collection<Candidate> candidates) {
        return candidates.stream().map(Candidate::notionKey).distinct().count() >= LEVELS.size();
    }

    /**
     * Tire les questions d'un défi, de la plus facile à la plus difficile.
     *
     * @param recentCards les cartes des derniers défis du sujet : écartées, sauf s'il ne reste plus assez de notions
     *     sans elles
     * @throws IllegalArgumentException s'il n'y a pas assez de notions ({@link #canDraw})
     */
    public static List<UUID> draw(List<Candidate> candidates, Set<UUID> recentCards, long seed) {
        List<Candidate> fresh = candidates.stream()
                .filter(candidate -> !recentCards.contains(candidate.cardId()))
                .toList();
        List<Candidate> pool = canDraw(fresh) ? fresh : candidates;
        if (!canDraw(pool)) {
            throw new IllegalArgumentException("Pas assez de notions pour un défi : il en faut " + LEVELS.size());
        }
        List<Candidate> notions = oneCardPerNotion(pool, seed);
        List<Candidate> chosen = new ArrayList<>();
        Set<String> domains = new HashSet<>();
        List<Level> missing = new ArrayList<>();
        for (Level level : LEVELS) {
            pick(notions, domains, candidate -> candidate.level() == level)
                    .ifPresentOrElse(chosen::add, () -> missing.add(level));
        }
        for (Level level : missing) {
            chosen.add(closest(notions, domains, level));
        }
        // Le tri est stable : à niveau égal, l'ordre du tirage reste.
        chosen.sort(Comparator.comparing(Candidate::level));
        return chosen.stream().map(Candidate::cardId).toList();
    }

    /** Une carte au hasard de chaque notion, les notions dans un ordre au hasard. */
    private static List<Candidate> oneCardPerNotion(List<Candidate> pool, long seed) {
        List<Candidate> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(seed));
        Map<UUID, Candidate> byNotion = new LinkedHashMap<>();
        for (Candidate candidate : shuffled) {
            byNotion.putIfAbsent(candidate.notionKey(), candidate);
        }
        return new ArrayList<>(byNotion.values());
    }

    /** La notion du niveau le plus proche de celui qui manque : un niveau d'écart, puis deux. */
    private static Candidate closest(List<Candidate> notions, Set<String> domains, Level level) {
        for (int gap = 1; gap < Level.values().length; gap++) {
            int distance = gap;
            Optional<Candidate> picked = pick(
                    notions, domains, candidate -> Math.abs(candidate.level().ordinal() - level.ordinal()) == distance);
            if (picked.isPresent()) {
                return picked.get();
            }
        }
        throw new IllegalStateException("Plus aucune notion à tirer");
    }

    /**
     * Retire et rend la première notion qui convient, d'un domaine pas encore tiré si possible ; vide si aucune ne
     * convient.
     */
    private static Optional<Candidate> pick(List<Candidate> notions, Set<String> domains, Predicate<Candidate> fits) {
        Optional<Candidate> picked = notions.stream()
                .filter(fits)
                .filter(candidate -> candidate.domain() == null || !domains.contains(candidate.domain()))
                .findFirst()
                .or(() -> notions.stream().filter(fits).findFirst());
        picked.ifPresent(candidate -> {
            notions.remove(candidate);
            String domain = candidate.domain();
            if (domain != null) {
                domains.add(domain);
            }
        });
        return picked;
    }
}
