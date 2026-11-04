package com.recallio.content.domain.catalog;

import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
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

    /** Range les notions, déjà dans l'ordre du référentiel, sous leur domaine puis leur chapitre. */
    public static TourOutline of(String subjectSlug, String subjectTitle, List<TourEntry> entries) {
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
                domains.values().stream().map(DomainBuilder::build).toList());
    }

    public record Domain(String id, String title, List<Chapter> chapters) {

        public Domain {
            chapters = List.copyOf(chapters);
        }
    }

    public record Chapter(String id, String title, List<TourEntry> notions) {

        public Chapter {
            notions = List.copyOf(notions);
        }
    }

    private static final class DomainBuilder {
        private final TourEntry first;
        private final Map<String, List<TourEntry>> chapters = new LinkedHashMap<>();

        DomainBuilder(TourEntry first) {
            this.first = first;
        }

        Domain build() {
            return new Domain(
                    first.domain(),
                    first.domainTitle(),
                    chapters.values().stream()
                            .map(notions -> new Chapter(
                                    notions.getFirst().chapter(),
                                    notions.getFirst().chapterTitle(),
                                    notions))
                            .toList());
        }
    }
}
