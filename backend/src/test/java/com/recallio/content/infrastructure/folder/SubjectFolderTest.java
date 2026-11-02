package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.domain.source.ParsedContent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contrôle de tous les dossiers de sujet de {@code content/} (ADR 0026) dans la CI (job {@code content}) : un fichier de
 * notion fautif se voit avant le merge, pas dans le rapport d'import. Un nouveau dossier est vérifié dès qu'il a un
 * {@code subject.yaml}, sans modifier ce test.
 */
class SubjectFolderTest {

    private static final Path CONTENT = Path.of("..", "content");

    static List<Path> folders() {
        return SubjectFolders.in(CONTENT);
    }

    @ParameterizedTest(allowZeroInvocations = true)
    @MethodSource("folders")
    void shouldBeNamedAfterItsSubject(Path folder) {
        assertThat(parse(folder).subject().slug())
                .isEqualTo(folder.getFileName().toString());
    }

    @ParameterizedTest(allowZeroInvocations = true)
    @MethodSource("folders")
    void shouldParseWithoutIssues(Path folder) {
        ParsedContent parsed = parse(folder);

        assertThat(parsed.issues()).isEmpty();
        assertThat(parsed.cardCount()).isPositive();
    }

    /** Un sujet a une seule source : sinon, chaque import retirerait les cartes de l'autre. */
    @ParameterizedTest(allowZeroInvocations = true)
    @MethodSource("folders")
    void shouldNotAlsoHaveAMarkdownDocument(Path folder) {
        Path document = CONTENT.resolve("source").resolve(folder.getFileName() + "-entretien-reference.md");

        assertThat(document).doesNotExist();
        assertThat(Files.isDirectory(folder.resolve("referentiel"))).isTrue();
    }

    private static ParsedContent parse(Path folder) {
        return new YamlSubjectFolderParser().parse(SubjectFolders.read(folder));
    }
}
