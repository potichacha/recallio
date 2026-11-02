package com.recallio.content.referential;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Valide le référentiel de chaque sujet ({@code content/<sujet>/referentiel/*.yaml}, ADR 0025) : l'inventaire de
 * toutes ses notions. Un sujet est vérifié dès que son dossier existe, sans modifier ce test. Les propriétés inconnues
 * font échouer la lecture : une faute de frappe dans un nom de champ se voit.
 */
class ReferentialTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Set<String> LEVELS = Set.of("beginner", "intermediate", "expert");
    private static final List<String> LEVEL_ORDER = List.of("beginner", "intermediate", "expert");
    private static final Set<String> FORMATS = Set.of(
            "course",
            "true-false",
            "multiple-choice",
            "fill-in",
            "code-output",
            "bug-fix",
            "scenario",
            "definition",
            "exercise");
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 4;

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    static Stream<Path> referentials() throws IOException {
        try (Stream<Path> subjects = Files.list(CONTENT)) {
            List<Path> found = subjects.map(subject -> subject.resolve("referentiel"))
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
            assertThat(found).as("référentiels de contenu").isNotEmpty();
            return found.stream();
        }
    }

    @ParameterizedTest
    @MethodSource("referentials")
    void shouldDescribeEveryNotionWithAValidLevelPriorityAndFormats(Path referential) throws IOException {
        List<Notion> notions = notions(read(referential));

        assertThat(notions).as("notions de %s", referential).isNotEmpty();
        for (Notion notion : notions) {
            assertThat(notion.id()).as("identifiant").matches(SLUG);
            assertThat(notion.title()).as("titre de %s", notion.id()).isNotBlank();
            assertThat(notion.level()).as("niveau de %s", notion.id()).isIn(LEVELS);
            assertThat(notion.priority()).as("priorité de %s", notion.id()).isBetween(MIN_PRIORITY, MAX_PRIORITY);
            assertThat(FORMATS).as("formats de %s", notion.id()).containsAll(notion.formatsOrEmpty());
        }
    }

    @ParameterizedTest
    @MethodSource("referentials")
    void shouldUseUniqueIdentifiersAndPositions(Path referential) throws IOException {
        List<DomainFile> domains = read(referential);

        assertThat(domains)
                .extracting(DomainFile::domain)
                .doesNotHaveDuplicates()
                .allMatch(SLUG.asPredicate());
        assertThat(domains).extracting(DomainFile::position).doesNotHaveDuplicates();
        assertThat(domains.stream()
                        .flatMap(domain -> domain.chapters().stream())
                        .map(Chapter::id))
                .doesNotHaveDuplicates()
                .allMatch(SLUG.asPredicate());
        assertThat(notions(domains)).extracting(Notion::id).doesNotHaveDuplicates();
    }

    /**
     * Un prérequis existe, n'est jamais d'un niveau plus avancé ni moins essentiel que la notion qui en dépend : sinon
     * le parcours « apprendre » bloquerait un débutant sur une notion d'expert, ou une notion essentielle sur une rare.
     */
    @ParameterizedTest
    @MethodSource("referentials")
    void shouldOnlyRequireExistingEarlierAndMoreEssentialNotions(Path referential) throws IOException {
        Map<String, Notion> byId = new HashMap<>();
        notions(read(referential)).forEach(notion -> byId.put(notion.id(), notion));

        List<String> problems = new ArrayList<>();
        for (Notion notion : byId.values()) {
            for (String required : notion.requiresOrEmpty()) {
                Notion prerequisite = byId.get(required);
                if (prerequisite == null) {
                    problems.add(notion.id() + " requiert « " + required + " », qui n'existe pas");
                } else if (LEVEL_ORDER.indexOf(prerequisite.level()) > LEVEL_ORDER.indexOf(notion.level())) {
                    problems.add(notion.id() + " (" + notion.level() + ") requiert " + required + " ("
                            + prerequisite.level() + ")");
                } else if (prerequisite.priority() > notion.priority()) {
                    problems.add(notion.id() + " (priorité " + notion.priority() + ") requiert " + required
                            + " (priorité " + prerequisite.priority() + ")");
                }
            }
        }
        assertThat(problems).as("prérequis incohérents").isEmpty();
    }

    @ParameterizedTest
    @MethodSource("referentials")
    void shouldHaveNoCycleInPrerequisites(Path referential) throws IOException {
        Map<String, List<String>> graph = new HashMap<>();
        notions(read(referential)).forEach(notion -> graph.put(notion.id(), notion.requiresOrEmpty()));

        Set<String> done = new HashSet<>();
        for (String id : graph.keySet()) {
            List<String> cycle = cycleFrom(id, graph, new ArrayList<>(), done);
            assertThat(cycle).as("cycle de prérequis").isEmpty();
        }
    }

    /** Parcours en profondeur : le chemin en cours révèle un cycle s'il revient sur lui-même. */
    private static List<String> cycleFrom(
            String id, Map<String, List<String>> graph, List<String> path, Set<String> done) {
        if (path.contains(id)) {
            List<String> cycle = new ArrayList<>(path.subList(path.indexOf(id), path.size()));
            cycle.add(id);
            return cycle;
        }
        if (!done.add(id)) {
            return List.of();
        }
        path.add(id);
        for (String next : graph.getOrDefault(id, List.of())) {
            List<String> cycle = cycleFrom(next, graph, path, done);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        path.removeLast();
        return List.of();
    }

    private static List<DomainFile> read(Path referential) throws IOException {
        try (Stream<Path> files = Files.list(referential)) {
            List<DomainFile> domains = new ArrayList<>();
            for (Path file : files.filter(path -> path.toString().endsWith(".yaml"))
                    .sorted()
                    .toList()) {
                domains.add(YAML.readValue(file.toFile(), DomainFile.class));
            }
            return domains;
        }
    }

    private static List<Notion> notions(List<DomainFile> domains) {
        return domains.stream()
                .flatMap(domain -> domain.chapters().stream())
                .flatMap(chapter -> chapter.notions().stream())
                .toList();
    }

    record DomainFile(String domain, String title, int position, List<Chapter> chapters) {}

    record Chapter(String id, String title, List<Notion> notions) {}

    record Notion(
            String id,
            String title,
            String level,
            int priority,
            @Nullable List<String> requires,
            @Nullable List<String> formats) {

        List<String> requiresOrEmpty() {
            return requires == null ? List.of() : requires;
        }

        List<String> formatsOrEmpty() {
            return formats == null ? List.of() : formats;
        }
    }
}
