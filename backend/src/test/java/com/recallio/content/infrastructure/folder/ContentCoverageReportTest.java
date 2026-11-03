package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.content.domain.source.ContentCoverage;
import com.recallio.content.domain.source.ContentCoverage.ChapterExercises;
import com.recallio.content.domain.source.ContentCoverage.NotionCoverage;
import com.recallio.content.domain.source.ContentCoverage.PriorityCoverage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Publie la couverture du référentiel de chaque dossier de sujet (ADR 0025) : dans le résumé du job GitHub
 * ({@code GITHUB_STEP_SUMMARY}) en CI, dans {@code target/content-coverage.md} en local. Une mesure, pas une règle : ce
 * test n'échoue que si le rapport ne peut pas être produit.
 */
class ContentCoverageReportTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final Path LOCAL_REPORT = Path.of("target", "content-coverage.md");
    private static final String SUMMARY_VARIABLE = "GITHUB_STEP_SUMMARY";
    private static final int PERCENT = 100;

    /** Les colonnes de la matrice, dans l'ordre du guide de rédaction ({@code content/README.md}). */
    private static final List<String> FORMATS = List.of(
            "course", "true-false", "multiple-choice", "fill-in", "code-output", "bug-fix", "scenario", "definition");

    private static final Map<Integer, String> PRIORITIES =
            Map.of(1, "1 essentiel", 2, "2 courant", 3, "3 avancé", 4, "4 rare");

    @Test
    void shouldPublishTheCoverageOfEverySubjectFolder() throws IOException {
        String report = SubjectFolders.in(CONTENT).stream()
                .map(folder -> new YamlSubjectFolderParser().coverage(SubjectFolders.read(folder)))
                .map(ContentCoverageReportTest::markdown)
                .collect(Collectors.joining("\n"));

        assertThat(report).isNotBlank();
        write(report);
    }

    private static String markdown(ContentCoverage coverage) {
        var report = new StringBuilder()
                .append("## Couverture du contenu : ")
                .append(coverage.subject())
                .append("\n\nUne notion est complète quand chacun de ses formats a au moins ")
                .append(ContentCoverage.TARGET_PER_FORMAT)
                .append(" cartes (ADR 0025).\n\n")
                .append("| Priorité | Notions | Avec des cartes | Complètes |\n|---|---:|---:|---:|\n");
        for (PriorityCoverage priority : coverage.byPriority()) {
            report.append("| %s | %d | %s | %s |\n"
                    .formatted(
                            PRIORITIES.get(priority.priority()),
                            priority.notions(),
                            share(priority.covered(), priority.notions()),
                            share(priority.complete(), priority.notions())));
        }
        appendFormats(report, coverage);
        long declared = coverage.notions().stream()
                .filter(NotionCoverage::declaredComplete)
                .count();
        report.append("\n**Notions déclarées complètes** (`complete: true`, vérifiées par la CI) : ")
                .append(declared)
                .append(".\n");
        ChapterExercises exercises = coverage.chapterExercises();
        report.append("\n**Exercices de code** : %d. Chapitres qui en ont au moins un, comme visé : %s sur %d.\n"
                .formatted(
                        exercises.exercises(),
                        share(exercises.chaptersWithExercise(), exercises.chapters()),
                        exercises.chapters()));
        appendUncovered(report, coverage);
        appendMatrix(report, coverage);
        return report.toString();
    }

    private static void appendFormats(StringBuilder report, ContentCoverage coverage) {
        report.append("\n### Par format\n\n| Format | Notions qui le retiennent | À la cible | Cartes |\n")
                .append("|---|---:|---:|---:|\n");
        for (String format : FORMATS) {
            CardType type = type(format);
            List<NotionCoverage> retaining = coverage.notions().stream()
                    .filter(notion -> notion.formats().contains(type))
                    .toList();
            long atTarget = retaining.stream()
                    .filter(notion -> notion.count(type) >= ContentCoverage.TARGET_PER_FORMAT)
                    .count();
            int cards = coverage.notions().stream()
                    .mapToInt(notion -> notion.count(type))
                    .sum();
            report.append("| %s | %d | %s | %d |\n"
                    .formatted(format, retaining.size(), share((int) atTarget, retaining.size()), cards));
        }
    }

    private static void appendUncovered(StringBuilder report, ContentCoverage coverage) {
        List<NotionCoverage> uncovered = coverage.uncovered(1);
        report.append("\n### Notions essentielles sans carte (")
                .append(uncovered.size())
                .append(")\n\n");
        uncovered.forEach(notion -> report.append("- `%s` (%s) : %s\n"
                .formatted(
                        notion.notion().slug(),
                        notion.notion().domain(),
                        notion.notion().title())));
    }

    /** Le détail par notion, replié : plusieurs centaines de lignes pour un sujet complet. */
    private static void appendMatrix(StringBuilder report, ContentCoverage coverage) {
        report.append("\n<details><summary>Matrice par notion</summary>\n\n| Notion | P | ")
                .append(String.join(" | ", FORMATS))
                .append(" |\n|---|---:|")
                .append("---:|".repeat(FORMATS.size()))
                .append('\n');
        for (NotionCoverage notion : coverage.notions()) {
            report.append("| `%s` | %d | "
                            .formatted(notion.notion().slug(), notion.notion().priority()))
                    .append(FORMATS.stream().map(format -> cell(notion, format)).collect(Collectors.joining(" | ")))
                    .append(" |\n");
        }
        report.append("\n</details>\n");
    }

    /** {@code ·} pour un format que la notion ne retient pas ; le nombre de cartes, en gras à la cible. */
    private static String cell(NotionCoverage notion, String format) {
        CardType type = type(format);
        if (!notion.formats().contains(type)) {
            return "·";
        }
        int count = notion.count(type);
        return count >= ContentCoverage.TARGET_PER_FORMAT ? "**" + count + "**" : String.valueOf(count);
    }

    private static CardType type(String format) {
        return Objects.requireNonNull(NotionCards.SUPPORTED.get(format), format);
    }

    private static String share(int part, int total) {
        return total == 0 ? "0" : "%d (%d %%)".formatted(part, part * PERCENT / total);
    }

    /** Ajoute au résumé du job en CI (d'autres étapes peuvent y écrire) ; remplace le rapport local. */
    private static void write(String report) throws IOException {
        String summary = System.getenv(SUMMARY_VARIABLE);
        if (summary != null && !summary.isBlank()) {
            Files.writeString(
                    Path.of(summary),
                    report,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } else {
            Files.createDirectories(Path.of("target"));
            Files.writeString(LOCAL_REPORT, report, StandardCharsets.UTF_8);
        }
    }
}
