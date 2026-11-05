package com.recallio.content.domain.catalog;

import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
import com.recallio.content.domain.catalog.ContentCatalog.TourExercise;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le sommaire du Tour d'un sujet (ADR 0037), rangé comme son référentiel : par domaine, puis par chapitre. L'ordre des
 * notions est celui du référentiel, et la PWA en déduit la page précédente et la suivante.
 *
 * @param domains vide si le sujet n'a pas encore de Tour
 */
public record TourOutline(String subjectSlug, String subjectTitle, List<Domain> domains) {

    public TourOutline {
        domains = List.copyOf(domains);
    }

    /**
     * Range les notions, déjà dans l'ordre du référentiel, sous leur domaine puis leur chapitre. Chaque chapitre reçoit
     * ses exercices de code ; ceux des chapitres absents du Tour sont écartés.
     */
    public static TourOutline of(
            String subjectSlug, String subjectTitle, List<TourEntry> entries, List<TourExercise> exercises) {
        Map<String, DomainBuilder> domains = new LinkedHashMap<>();
        for (TourEntry entry : entries) {
            domains.computeIfAbsent(entry.domain(), _ -> new DomainBuilder(entry))
                    .chapters
                    .computeIfAbsent(entry.chapter(), _ -> new ArrayList<>())
                    .add(entry);
        }
        return new TourOutline(
                subjectSlug,
                subjectTitle,
                domains.values().stream().map(domain -> domain.build(exercises)).toList());
    }

    public record Domain(String id, String title, List<Chapter> chapters) {

        public Domain {
            chapters = List.copyOf(chapters);
        }
    }

    /** @param exercises les exercices de code du chapitre, à la fin de ses pages */
    public record Chapter(String id, String title, List<TourEntry> notions, List<TourExercise> exercises) {

        public Chapter {
            notions = List.copyOf(notions);
            exercises = List.copyOf(exercises);
        }
    }

    private static final class DomainBuilder {
        private final TourEntry first;
        private final Map<String, List<TourEntry>> chapters = new LinkedHashMap<>();

        DomainBuilder(TourEntry first) {
            this.first = first;
        }

        Domain build(List<TourExercise> exercises) {
            return new Domain(
                    first.domain(),
                    first.domainTitle(),
                    chapters.values().stream()
                            .map(notions -> new Chapter(
                                    notions.getFirst().chapter(),
                                    notions.getFirst().chapterTitle(),
                                    notions,
                                    exercises.stream()
                                            .filter(exercise ->
                                                    exercise.domain().equals(first.domain())
                                                            && exercise.chapter()
                                                                    .equals(notions.getFirst()
                                                                            .chapter()))
                                            .toList()))
                            .toList());
        }
    }
}
