package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.content.domain.source.ContentQuality;
import com.recallio.content.domain.source.ContentQuality.Rule;
import com.recallio.content.domain.source.ContentQuality.Violation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Les règles de rédaction des cartes ({@link ContentQuality}), appliquées à chaque dossier de sujet dans la CI (job
 * {@code content}). Le contenu écrit avant ces règles les enfreint : {@code content/quality-baseline.yaml} note, par
 * sujet et par règle, le nombre d'infractions tolérées. C'est un cliquet : ce nombre ne peut que baisser, et un sujet qui
 * n'y figure pas n'en tolère aucune.
 */
class ContentQualityBaselineTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final Path BASELINE = CONTENT.resolve("quality-baseline.yaml");
    private static final Path LOCAL_REPORT = Path.of("target", "content-quality.md");
    private static final String SUMMARY_VARIABLE = "GITHUB_STEP_SUMMARY";

    /** Les infractions citées quand un plafond est dépassé : assez pour corriger, sans noyer le journal. */
    private static final int SHOWN = 30;

    static List<Path> folders() {
        return SubjectFolders.in(CONTENT);
    }

    @ParameterizedTest(allowZeroInvocations = true)
    @MethodSource("folders")
    void shouldNotBreakMoreWritingRulesThanItsBaseline(Path folder) throws IOException {
        ContentQuality quality = quality(folder);
        Map<String, Integer> tolerated = baseline().getOrDefault(quality.subject(), Map.of());

        for (Rule rule : Rule.values()) {
            int ceiling = tolerated.getOrDefault(rule.key(), 0);
            assertThat(quality.count(rule))
                    .as(
                            "%s, règle %s : au plus %d infraction(s) (content/quality-baseline.yaml, content/README.md)."
                                    + " Les dernières relevées :%n%s",
                            quality.subject(), rule.key(), ceiling, shown(quality.violations(rule)))
                    .isLessThanOrEqualTo(ceiling);
            assertThat(quality.count(rule))
                    .as(
                            "%s, règle %s : il reste moins d'infractions que le plafond. Abaisse-le dans"
                                    + " content/quality-baseline.yaml, pour que le progrès soit acquis",
                            quality.subject(), rule.key())
                    .isEqualTo(ceiling);
        }
    }

    /** Un plafond ne vaut que pour un sujet qui existe et une règle connue : une faute de frappe ne tolère rien. */
    @Test
    void shouldOnlyNameKnownSubjectsAndRules() throws IOException {
        List<String> subjects = folders().stream()
                .map(folder -> folder.getFileName().toString())
                .toList();
        List<String> rules = List.of(Rule.values()).stream().map(Rule::key).toList();

        assertThat(baseline().keySet()).isSubsetOf(subjects);
        baseline().values().forEach(ceilings -> {
            assertThat(ceilings.keySet()).isSubsetOf(rules);
            assertThat(ceilings.values()).allMatch(ceiling -> ceiling > 0, "un plafond nul ne s'écrit pas");
        });
    }

    /** Dans le résumé du job en CI, dans {@code target/content-quality.md} en local, avec chaque infraction. */
    @Test
    void shouldPublishTheWritingQualityOfEverySubjectFolder() throws IOException {
        var report = new StringBuilder("## Qualité de rédaction des cartes\n\n| Sujet | ")
                .append(List.of(Rule.values()).stream().map(Rule::key).collect(Collectors.joining(" | ")))
                .append(" |\n|---|")
                .append("---:|".repeat(Rule.values().length))
                .append('\n');
        List<ContentQuality> qualities =
                folders().stream().map(ContentQualityBaselineTest::quality).toList();
        for (ContentQuality quality : qualities) {
            report.append("| ")
                    .append(quality.subject())
                    .append(" | ")
                    .append(List.of(Rule.values()).stream()
                            .map(rule -> String.valueOf(quality.count(rule)))
                            .collect(Collectors.joining(" | ")))
                    .append(" |\n");
        }
        String summary = System.getenv(SUMMARY_VARIABLE);
        if (summary != null && !summary.isBlank()) {
            Files.writeString(
                    Path.of(summary),
                    report.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            return;
        }
        for (ContentQuality quality : qualities) {
            report.append("\n### ").append(quality.subject()).append("\n\n");
            quality.violations().forEach(violation -> report.append(line(violation)));
        }
        Files.createDirectories(LOCAL_REPORT.getParent());
        Files.writeString(LOCAL_REPORT, report.toString(), StandardCharsets.UTF_8);
    }

    private static ContentQuality quality(Path folder) {
        return ContentQuality.of(new YamlSubjectFolderParser().parse(SubjectFolders.read(folder)));
    }

    private static Map<String, Map<String, Integer>> baseline() throws IOException {
        Map<String, Map<String, Integer>> baseline =
                new ObjectMapper(new YAMLFactory()).readValue(BASELINE.toFile(), new TypeReference<>() {});
        return baseline == null ? Map.of() : baseline;
    }

    private static String shown(List<Violation> violations) {
        return violations.stream()
                .skip(Math.max(0, violations.size() - SHOWN))
                .map(ContentQualityBaselineTest::line)
                .collect(Collectors.joining());
    }

    private static String line(Violation violation) {
        return "- `%s` (%s) : %s%n".formatted(violation.card(), violation.rule().key(), violation.detail());
    }
}
