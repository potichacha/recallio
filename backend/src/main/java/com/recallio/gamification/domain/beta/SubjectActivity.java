package com.recallio.gamification.domain.beta;

import com.recallio.platform.domain.UsageStatistics.SubjectCount;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * L'activité d'un sujet sur la fenêtre (ADR 0076).
 *
 * @param title son titre ; {@code null} s'il n'est pas publié
 * @param followers les comptes qui le suivent, à ce jour
 * @param reviewers les comptes qui ont révisé au moins une de ses cartes
 * @param reviews les réponses données à ses cartes
 * @param recalled celles qui sont justes
 */
public record SubjectActivity(
        String slug, @Nullable String title, long followers, long reviewers, long reviews, long recalled) {

    /** Le plus révisé d'abord ; à égalité, le plus suivi, puis l'ordre des slugs. */
    private static final Comparator<SubjectActivity> BY_ACTIVITY = Comparator.comparingLong(SubjectActivity::reviews)
            .thenComparingLong(SubjectActivity::followers)
            .reversed()
            .thenComparing(SubjectActivity::slug);

    /** La part des réponses justes, en pourcentage arrondi vers le bas ; aucune sans réponse. */
    public @Nullable Integer successPercent() {
        return BetaReport.percent(recalled, reviews);
    }

    /**
     * Chaque sujet publié, et tout autre sujet suivi ou révisé (un brouillon, un sujet dépublié), le plus révisé
     * d'abord.
     *
     * @param publishedTitles le titre de chaque sujet publié, par slug
     */
    public static List<SubjectActivity> ranked(
            Map<String, String> publishedTitles, Map<SubjectCount, Map<String, Long>> counts) {
        Set<String> slugs = new TreeSet<>(publishedTitles.keySet());
        counts.values().forEach(bySubject -> slugs.addAll(bySubject.keySet()));
        return slugs.stream()
                .map(slug -> new SubjectActivity(
                        slug,
                        publishedTitles.get(slug),
                        count(counts, SubjectCount.FOLLOWERS, slug),
                        count(counts, SubjectCount.REVIEWERS, slug),
                        count(counts, SubjectCount.REVIEWS, slug),
                        count(counts, SubjectCount.RECALLED, slug)))
                .sorted(BY_ACTIVITY)
                .toList();
    }

    private static long count(Map<SubjectCount, Map<String, Long>> counts, SubjectCount which, String slug) {
        return counts.getOrDefault(which, Map.of()).getOrDefault(slug, 0L);
    }
}
