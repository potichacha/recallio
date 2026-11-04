package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.runner.sandbox.Submission.SourceFile;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProgramTest {

    @Test
    void shouldRequireAMainFileForJava() {
        assertThatThrownBy(() -> new Program(Language.JAVA, List.of(new SourceFile("Point.java", "record Point() {}"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Main.java");
    }

    @Test
    void shouldRefuseATestFileInAGoProgram() {
        assertThatThrownBy(() -> new Program(Language.GO, List.of(new SourceFile("main_test.go", "package main"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRefuseAFileOfAnotherLanguage() {
        assertThatThrownBy(() -> new Program(Language.GO, List.of(new SourceFile("Main.java", "void main() {}"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBoundTheNumberOfFiles() {
        List<SourceFile> files = IntStream.rangeClosed(0, Submission.MAX_FILES_PER_PART)
                .mapToObj(index -> new SourceFile("fichier%d.go".formatted(index), "package main"))
                .toList();

        assertThatThrownBy(() -> new Program(Language.GO, files)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Program(Language.GO, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
