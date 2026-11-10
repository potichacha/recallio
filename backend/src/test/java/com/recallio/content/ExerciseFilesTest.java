package com.recallio.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.ExerciseFiles.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExerciseFilesTest {

    @Test
    void shouldTakeTheLanguageOfItsFiles() {
        assertThat(exercise("fr/recallio/Panier.java", "fr/recallio/PanierTest.java")
                        .language())
                .isEqualTo(CodeLanguage.JAVA);
        assertThat(exercise("panier.go", "panier_test.go").language()).isEqualTo(CodeLanguage.GO);
        assertThat(exercise("panier.cpp", "panier_test.cpp").language()).isEqualTo(CodeLanguage.CPP);
        assertThat(exercise("panier.hpp", "panier_test.cpp").language()).isEqualTo(CodeLanguage.CPP);
    }

    @Test
    void shouldRefuseAnExerciseThatMixesLanguages() {
        assertThatThrownBy(() -> exercise("Panier.java", "panier_test.go"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("un seul langage");
    }

    @Test
    void shouldKeepCppFilesFlatAndNameOnlyTheTestsAfterThem() {
        assertThatThrownBy(() -> exercise("panier/panier.cpp", "panier_test.cpp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chemin invalide");
        assertThatThrownBy(() -> exercise("panier.cpp", "verifications.cpp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("*_test.cpp");
    }

    @Test
    void shouldKeepGoFilesFlatAndNameOnlyTheTestsAfterThem() {
        assertThatThrownBy(() -> exercise("panier/panier.go", "panier_test.go"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chemin invalide");
        assertThatThrownBy(() -> exercise("panier.go", "verifications.go"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("*_test.go");
        assertThatThrownBy(() -> exercise("panier_test.go", "verifications_test.go"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seuls les fichiers de tests");
    }

    @Test
    void shouldRefuseAnotherExtension() {
        assertThatThrownBy(() -> new SourceFile("panier.py", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    private static ExerciseFiles exercise(String code, String test) {
        List<SourceFile> starter = List.of(new SourceFile(code, "// départ"));
        return new ExerciseFiles(
                starter, List.of(new SourceFile(code, "// solution")), List.of(new SourceFile(test, "// tests")));
    }
}
