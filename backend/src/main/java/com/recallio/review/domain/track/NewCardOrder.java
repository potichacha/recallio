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
 * <p>Les notions <em>connues</em>, montrées au test de niveau (ADR 0068), passent après les autres : en « apprendre »,
 * avant tout le reste de la clé, pour ne pas revoir les bases avant la première notion intermédiaire ; en « préparer un
 * entretien », après la priorité. « Entretenir son niveau » met déjà les notions débutantes en dernier.
 *
 * <p>Une carte sans notion (document Markdown) garde sa place : toutes les cartes d'un tel sujet sont à égalité.
 */
public final class NewCardOrder {

    /** La priorité d'une carte sans notion : à égalité avec toutes les autres cartes de son sujet. */
    private static final int NO_PRIORITY = 0;

    /** Dans une clé, ce qui passe d'abord, puis ce qui passe en dernier. */
    private static final int FIRST = 0;

    private static final int LAST = 1;

    private NewCardOrder() {}

    /**
     * @param eligible les cartes éligibles, dans l'ordre du programme
     * @param seen les cartes que l'utilisateur a déjà vues (elles ne sont jamais écartées)
     * @param tracks le parcours de chaque sujet ; un sujet absent suit {@link LearningTrack#DEFAULT}
     * @param notions les notions des sujets, par identifiant
     * @param locked les notions verrouillées pour le parcours « apprendre »
     * @param knownNotions les notions que l'utilisateur a montré connaître au test de niveau
     */
    public static List<Candidate> arrange(
            List<Candidate> eligible,
            Set<UUID> seen,
            Map<String, LearningTrack> tracks,
            Map<UUID, NotionInfo> notions,
            Set<UUID> locked,
            Set<UUID> knownNotions) {
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
                    return new Ranked(card, index, key(track, notion(card, notions), knownNotions));
                })
                .filter(ranked ->
                        seen.contains(ranked.card().cardId()) || !isLockedForLearning(ranked.card(), tracks, locked))
                .sorted(order)
                .map(Ranked::card)
                .toList();
    }

    /**
     * L'ordre des notions d'un sujet dans un parcours : celui où leurs nouvelles cartes sont servies. L'ordre du
     * référentiel départage les égalités, comme l'ordre du programme départage les cartes. Le compte à rebours avant
     * un entretien (ADR 0066) range ainsi ses notions comme le parcours « préparer un entretien » range leurs cartes.
     *
     * @param knownNotions les notions que l'utilisateur a montré connaître au test de niveau
     */
    public static Comparator<NotionInfo> notionOrder(LearningTrack track, Set<UUID> knownNotions) {
        return Comparator.<NotionInfo, int[]>comparing(
                        notion -> key(track, notion, knownNotions), NewCardOrder::compareKeys)
                .thenComparingInt(NotionInfo::position);
    }

    private static boolean isLockedForLearning(Candidate card, Map<String, LearningTrack> tracks, Set<UUID> locked) {
        UUID notion = card.notionId();
        return tracks.get(card.subjectSlug()) == LearningTrack.LEARN && notion != null && locked.contains(notion);
    }

    /** La clé de tri d'une carte dans son sujet ; l'ordre du programme départage les égalités. */
    private static int[] key(LearningTrack track, @Nullable NotionInfo notion, Set<UUID> knownNotions) {
        if (notion == null) {
            return new int[] {NO_PRIORITY};
        }
        int level = notion.level().ordinal();
        int known = knownNotions.contains(notion.id()) ? LAST : FIRST;
        return switch (track) {
            case LEARN -> new int[] {known, level};
            case PREPARE -> new int[] {notion.priority(), known};
            case MAINTAIN -> new int[] {notion.level() == NotionInfo.Level.BEGINNER ? LAST : FIRST, notion.priority()};
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
