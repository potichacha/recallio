package com.recallio.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.ExerciseFiles.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourPageTest {

    private static final SourceFile MAIN_GO = new SourceFile("main.go", "package main\n\nfunc main() {}\n");
    private static final SourceFile MAIN_JAVA = new SourceFile("Main.java", "void main() {}\n");

    @Test
    void shouldAcceptAPageWithItsExampleAndItsTask() {
        var page = new TourPage(
                "Bonjour",
                "Le texte de la page.",
                program(MAIN_GO),
                new TourPage.Task("Affiche « Salut ».", List.of(MAIN_GO), List.of(MAIN_GO), "Salut\n"));

        assertThat(page.language()).isEqualTo(CodeLanguage.GO);
    }

    @Test
    void shouldRequireTheMainFileOfAJavaProgram() {
        assertThatThrownBy(() -> program(new SourceFile("Point.java", "record Point(int x, int y) {}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Main.java");
    }

    @Test
    void shouldRefuseATestFileOrAProgramInTwoLanguages() {
        assertThatThrownBy(() -> program(new SourceFile("main_test.go", "package main\n")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> program(MAIN_JAVA, MAIN_GO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("un seul langage");
    }

    @Test
    void shouldRequireATaskWithTheFilesOfItsStarterInTheLanguageOfTheExample() {
        var other = new SourceFile("autre.go", "package main\n");

        assertThatThrownBy(() -> new TourPage.Task("Consigne", List.of(MAIN_GO), List.of(MAIN_GO, other), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactement les fichiers");
        assertThatThrownBy(() -> new TourPage(
                        "Titre",
                        "Texte",
                        program(MAIN_GO),
                        new TourPage.Task("Consigne", List.of(MAIN_JAVA), List.of(MAIN_JAVA), "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("langage de l'exemple");
    }

    @Test
    void shouldRefuseAnEmptyTextOrOutputAndATitleTooLong() {
        assertThatThrownBy(() -> new TourPage(null, " ", program(MAIN_GO), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TourPage.Program(List.of(MAIN_GO), ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new TourPage("x".repeat(TourPage.MAX_TITLE_LENGTH + 1), "Texte", program(MAIN_GO), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TourPage.Program program(SourceFile... files) {
        return new TourPage.Program(List.of(files), "sortie\n");
    }
}
