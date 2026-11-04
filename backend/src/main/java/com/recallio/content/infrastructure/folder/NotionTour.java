package com.recallio.content.infrastructure.folder;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.TourPage;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.infrastructure.folder.FolderFiles.ProgramEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.TaskEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.TourPageEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Les pages du Tour des fichiers de notion (ADR 0037). Une page fautive est signalée dans le rapport et laissée de côté,
 * comme une carte fautive ; ce qui ne se voit qu'à l'exécution (la sortie annoncée) est vérifié par le runner, en CI.
 */
final class NotionTour {

    private NotionTour() {}

    /** Les pages valides d'un fichier de notion ; {@code path} est relatif à {@code notions/}. */
    static List<TourPage> read(String path, @Nullable List<TourPageEntry> entries, List<SourceIssue> issues) {
        if (entries == null) {
            return List.of();
        }
        List<TourPage> pages = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                pages.add(page(entries.get(index)));
            } catch (IllegalArgumentException e) {
                issues.add(new SourceIssue(
                        0, "notions/%s : page %d du Tour : %s".formatted(path, index + 1, e.getMessage()), path));
            }
        }
        return List.copyOf(pages);
    }

    /**
     * Le Tour d'un sujet suit l'ordre du référentiel : une notion vient après ceux de ses prérequis qui sont dans le Tour.
     * Toutes ses pages sont dans un même langage.
     */
    static void check(List<ParsedNotion> notions, List<SourceIssue> issues) {
        Map<String, Integer> positions = notions.stream()
                .filter(notion -> !notion.tour().isEmpty())
                .collect(Collectors.toMap(ParsedNotion::slug, ParsedNotion::position));
        for (ParsedNotion notion : notions) {
            if (notion.tour().isEmpty()) {
                continue;
            }
            for (String required : notion.requires()) {
                Integer position = positions.get(required);
                if (position != null && position > notion.position()) {
                    issues.add(new SourceIssue(
                            0,
                            "Tour : la notion « %s » vient avant son prérequis « %s » dans le référentiel"
                                    .formatted(notion.slug(), required),
                            notion.slug()));
                }
            }
        }
        Set<CodeLanguage> languages = notions.stream()
                .flatMap(notion -> notion.tour().stream())
                .map(TourPage::language)
                .collect(Collectors.toCollection(TreeSet::new));
        if (languages.size() > 1) {
            issues.add(
                    new SourceIssue(0, "Tour : les pages d'un sujet sont dans un seul langage, pas " + languages, ""));
        }
    }

    private static TourPage page(TourPageEntry entry) {
        String title = entry.title();
        TaskEntry task = entry.task();
        return new TourPage(
                title == null ? null : title.strip(),
                entry.text().strip(),
                program(entry.example()),
                task == null
                        ? null
                        : new TourPage.Task(
                                task.instructions().strip(),
                                files(task.starter()),
                                files(task.solution()),
                                task.output()));
    }

    private static TourPage.Program program(ProgramEntry entry) {
        return new TourPage.Program(files(entry.files()), entry.output());
    }

    /** Les fichiers dans l'ordre du fichier YAML, chacun terminé par un retour à la ligne, comme les écrit gofmt. */
    private static List<SourceFile> files(Map<String, String> files) {
        return files.entrySet().stream()
                .map(file -> new SourceFile(file.getKey(), withFinalNewline(file.getValue())))
                .toList();
    }

    private static String withFinalNewline(String content) {
        return content.endsWith("\n") ? content : content + "\n";
    }
}
