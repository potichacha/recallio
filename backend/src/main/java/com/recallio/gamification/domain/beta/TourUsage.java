package com.recallio.gamification.domain.beta;

import com.recallio.content.NotionInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * L'usage du Tour (ADR 0076). L'ouverture d'une page n'a pas d'histoire : seule la dernière page de chacun par sujet
 * est gardée (ADR 0037). Elle dit où chacun en est, pas combien de pages ont été lues.
 *
 * @param learners les comptes qui ont validé au moins une notion sur la fenêtre
 * @param stops par sujet, le chapitre de la dernière page ouverte de chacun, à ce jour
 */
public record TourUsage(int learners, List<SubjectStops> stops) {

    public TourUsage {
        stops = List.copyOf(stops);
    }

    /**
     * @param notions les notions des sujets publiés, dans l'ordre de leur référentiel
     * @param accountsByNotion par notion, les comptes dont la dernière page ouverte lui appartient ; une notion absente
     *     de {@code notions} (retirée, ou d'un sujet qui n'est pas publié) n'est pas comptée
     * @param subjectTitles le titre de chaque sujet publié, par slug
     */
    public static TourUsage of(
            int learners,
            List<NotionInfo> notions,
            Map<UUID, Long> accountsByNotion,
            Map<String, String> subjectTitles) {
        Map<String, Map<ChapterKey, Long>> bySubject = new LinkedHashMap<>();
        for (NotionInfo notion : notions) {
            if (notion.isInTour()) {
                bySubject
                        .computeIfAbsent(notion.subjectSlug(), _ -> new LinkedHashMap<>())
                        .merge(ChapterKey.of(notion), accountsByNotion.getOrDefault(notion.id(), 0L), Long::sum);
            }
        }
        List<SubjectStops> stops = new ArrayList<>();
        bySubject.forEach((subject, chapters) -> {
            if (chapters.values().stream().anyMatch(accounts -> accounts > 0)) {
                stops.add(new SubjectStops(subject, subjectTitles.getOrDefault(subject, subject), chapters(chapters)));
            }
        });
        return new TourUsage(learners, stops);
    }

    private static List<ChapterStops> chapters(Map<ChapterKey, Long> chapters) {
        return chapters.entrySet().stream()
                .map(entry -> new ChapterStops(
                        entry.getKey().domainTitle(), entry.getKey().chapterTitle(), entry.getValue()))
                .toList();
    }

    /** Un chapitre dans son sujet : deux domaines peuvent avoir un chapitre de même identifiant. */
    private record ChapterKey(String domain, String chapter, String domainTitle, String chapterTitle) {

        static ChapterKey of(NotionInfo notion) {
            return new ChapterKey(notion.domain(), notion.chapter(), notion.domainTitle(), notion.chapterTitle());
        }
    }

    /**
     * Un sujet où au moins un compte a ouvert une page du Tour.
     *
     * @param chapters tous les chapitres de son Tour, dans l'ordre du référentiel, même ceux où personne ne s'est arrêté
     */
    public record SubjectStops(String subjectSlug, String subjectTitle, List<ChapterStops> chapters) {

        public SubjectStops {
            chapters = List.copyOf(chapters);
        }
    }

    /** @param accounts les comptes dont la dernière page ouverte du sujet est dans ce chapitre */
    public record ChapterStops(String domainTitle, String chapterTitle, long accounts) {}
}
