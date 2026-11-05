package com.recallio.content.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
import com.recallio.content.domain.catalog.ContentCatalog.TourExercise;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TourOutlineTest {

    @Test
    void shouldGroupTheNotionsByDomainThenChapterInTheOrderOfTheReferential() {
        TourOutline outline = TourOutline.of(
                "go",
                "Go",
                List.of(
                        entry("bases", "premiers-pas", "hello-world"),
                        entry("bases", "premiers-pas", "packages-imports"),
                        entry("bases", "variables-types", "valeur-zero"),
                        entry("concurrence", "goroutines", "goroutines")),
                List.of());

        assertThat(outline.domains()).extracting(TourOutline.Domain::id).containsExactly("bases", "concurrence");
        assertThat(outline.domains().getFirst().chapters())
                .extracting(TourOutline.Chapter::id)
                .containsExactly("premiers-pas", "variables-types");
        assertThat(outline.domains().getFirst().chapters().getFirst().notions())
                .extracting(TourEntry::slug)
                .containsExactly("hello-world", "packages-imports");
        assertThat(outline.domains().getFirst().title()).isEqualTo("Domaine bases");
    }

    @Test
    void shouldPutEachExerciseAtTheEndOfItsChapterAndLeaveOutTheChaptersOutsideTheTour() {
        TourOutline outline = TourOutline.of(
                "go",
                "Go",
                List.of(entry("bases", "premiers-pas", "hello-world"), entry("bases", "boucles", "for")),
                List.of(
                        exercise("bases", "premiers-pas", "hello-world", "Saluer"),
                        exercise("bases", "boucles", "for", "Collatz"),
                        exercise("erreurs", "wrapping", "errors-is", "Envelopper")));

        assertThat(outline.domains().getFirst().chapters())
                .extracting(chapter ->
                        chapter.exercises().stream().map(TourExercise::summary).toList())
                .containsExactly(List.of("Saluer"), List.of("Collatz"));
    }

    @Test
    void shouldSummarizeAStatementByItsFirstLine() {
        assertThat(TourExercise.summary("Écris `Saluer`, qui prépare la phrase d'accueil :\n\n- `Saluer(\"Ada\")`"))
                .isEqualTo("Écris `Saluer`, qui prépare la phrase d'accueil");
        assertThat(TourExercise.summary("x".repeat(200))).hasSize(120).endsWith("…");
    }

    @Test
    void shouldHaveNoDomainWhenTheSubjectHasNoTour() {
        assertThat(TourOutline.of("java", "Java", List.of(), List.of()).domains())
                .isEmpty();
    }

    private static TourEntry entry(String domain, String chapter, String notion) {
        return new TourEntry(notion, domain, "Domaine " + domain, chapter, "Chapitre " + chapter, "Page " + notion, 1);
    }

    private static TourExercise exercise(String domain, String chapter, String notion, String summary) {
        return new TourExercise(domain, chapter, notion, UUID.randomUUID(), summary);
    }
}
