package com.recallio.content.infrastructure.markdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.InvalidSourceException;
import org.junit.jupiter.api.Test;

class SubjectHeaderTest {

    @Test
    void shouldReadTheSubjectAndSkipTheHeaderLines() {
        SubjectHeader.Read read = SubjectHeader.read(lines("""
                ---
                subject: spring-boot
                title: Spring Boot
                category: frameworks
                position: 30

                status: draft
                ---
                # Spring Boot : le document de référence
                """));

        assertThat(read.subject())
                .isEqualTo(new Subject(
                        "spring-boot", "Spring Boot", Subject.Category.FRAMEWORKS, 30, Subject.Status.DRAFT));
        assertThat(read.lineCount()).isEqualTo(8);
    }

    @Test
    void shouldAcceptKebabCaseCategories() {
        Subject subject =
                SubjectHeader.read(lines(header("category: cloud-devops"))).subject();

        assertThat(subject.category()).isEqualTo(Subject.Category.CLOUD_DEVOPS);
    }

    @Test
    void shouldRejectADocumentWithoutHeader() {
        assertThatThrownBy(() -> SubjectHeader.read(lines("# 1. JAVA : FONDAMENTAUX\n")))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("doit commencer par un en-tête");
    }

    @Test
    void shouldRejectAMissingKey() {
        assertThatThrownBy(() -> SubjectHeader.read(lines("---\nsubject: go\ntitle: Go\n---\n")))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("category");
    }

    @Test
    void shouldRejectAnUnknownKeyOrValue() {
        assertThatThrownBy(() -> SubjectHeader.read(lines(header("author: moi"))))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("author");
        assertThatThrownBy(() -> SubjectHeader.read(lines(header("category: cuisine"))))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("cuisine");
    }

    @Test
    void shouldRejectAnInvalidSlug() {
        assertThatThrownBy(() -> SubjectHeader.read(lines(header("subject: Spring Boot"))))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("Slug de sujet invalide");
    }

    @Test
    void shouldRejectAnUnclosedHeader() {
        assertThatThrownBy(() -> SubjectHeader.read(lines("---\nsubject: go\n")))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("refermé");
    }

    /** Un en-tête valide, dont une ligne est remplacée ou ajoutée. */
    private static String header(String override) {
        String key = override.substring(0, override.indexOf(':'));
        StringBuilder header = new StringBuilder("---\n");
        boolean replaced = false;
        for (String line :
                new String[] {"subject: go", "title: Go", "category: languages", "position: 20", "status: published"}) {
            if (line.startsWith(key + ":")) {
                header.append(override).append('\n');
                replaced = true;
            } else {
                header.append(line).append('\n');
            }
        }
        if (!replaced) {
            header.append(override).append('\n');
        }
        return header.append("---\n").toString();
    }

    private static String[] lines(String text) {
        return text.split("\\R", -1);
    }
}
