package com.recallio.review.domain.track;

import com.recallio.content.NotionInfo;
import com.recallio.review.domain.queue.ReviewQueuePlanner.Candidate;
import com.recallio.user.LearningTrack;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * Range les cartes éligibles selon le parcours de leur sujet (ADR 0032), avant que la file n'en tire les nouvelles
 * cartes. Les sujets gardent leur ordre, et les cartes dues ne sont pas concernées : la file les trie par retard.
 *
 * <ul>
 *   <li><b>Apprendre</b> : du débutant vers l'expert, dans l'ordre du programme ; les nouvelles cartes d'une notion
 *       verrouillée sont écartées.
 *   <li><b>Préparer un entretien</b> : par priorité (1 essentiel d'abord), dans l'ordre du programme.
 *   <li><b>Entretenir son niveau</b> : les notions intermédiaires et expertes d'abord, par priorité ; les débutantes en
 *       dernier.
 * </ul>
 *
 * <p>Une carte sans notion (document Markdown) garde sa place : toutes les cartes d'un tel sujet sont à égalité.
 */
public final class NewCardOrder {

    /** La priorité d'une carte sans notion : à égalité avec toutes les autres cartes de son sujet. */
    private static final int NO_PRIORITY = 0;

    private NewCardOrder() {}

    /**
     * @param eligible les cartes éligibles, dans l'ordre du programme
     * @param known les cartes que l'utilisateur a déjà vues (elles ne sont jamais écartées)
     * @param tracks le parcours de chaque sujet ; un sujet absent suit {@link LearningTrack#DEFAULT}
     * @param notions les notions des sujets, par identifiant
     * @param locked les notions verrouillées pour le parcours « apprendre »
     */
    public static List<Candidate> arrange(
            List<Candidate> eligible,
            Set<UUID> known,
            Map<String, LearningTrack> tracks,
            Map<UUID, NotionInfo> notions,
            Set<UUID> locked) {
        Map<String, Integer> subjectRank = new HashMap<>();
        eligible.forEach(card -> subjectRank.putIfAbsent(card.subjectSlug(), subjectRank.size()));
        record Ranked(Candidate card, int index, int[] key) {}
        Comparator<Ranked> order = Comparator.<Ranked>comparingInt(
                        ranked -> subjectRank.getOrDefault(ranked.card().subjectSlug(), 0))
                .thenComparing(Ranked::key, NewCardOrder::compareKeys)
                .thenComparingInt(Ranked::index);
        return IntStream.range(0, eligible.size())
                .mapToObj(index -> {
                    Candidate card = eligible.get(index);
                    LearningTrack track = tracks.getOrDefault(card.subjectSlug(), LearningTrack.DEFAULT);
                    return new Ranked(card, index, key(track, notion(card, notions)));
                })
                .filter(ranked ->
                        known.contains(ranked.card().cardId()) || !isLockedForLearning(ranked.card(), tracks, locked))
                .sorted(order)
                .map(Ranked::card)
                .toList();
    }

    private static boolean isLockedForLearning(Candidate card, Map<String, LearningTrack> tracks, Set<UUID> locked) {
        UUID notion = card.notionId();
        return tracks.get(card.subjectSlug()) == LearningTrack.LEARN && notion != null && locked.contains(notion);
    }

    /** La clé de tri d'une carte dans son sujet ; l'ordre du programme départage les égalités. */
    private static int[] key(LearningTrack track, @Nullable NotionInfo notion) {
        if (notion == null) {
            return new int[] {NO_PRIORITY};
        }
        int level = notion.level().ordinal();
        return switch (track) {
            case LEARN -> new int[] {level};
            case PREPARE -> new int[] {notion.priority()};
            case MAINTAIN -> new int[] {notion.level() == NotionInfo.Level.BEGINNER ? 1 : 0, notion.priority()};
        };
    }

    private static @Nullable NotionInfo notion(Candidate card, Map<UUID, NotionInfo> notions) {
        UUID notion = card.notionId();
        return notion == null ? null : notions.get(notion);
    }

    private static int compareKeys(int[] first, int[] second) {
        return Arrays.compare(first, second);
    }
}
