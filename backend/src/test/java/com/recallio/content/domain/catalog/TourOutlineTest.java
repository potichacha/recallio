package com.recallio.content.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
import java.util.List;
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
                        entry("concurrence", "goroutines", "goroutines")));

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
    void shouldHaveNoDomainWhenTheSubjectHasNoTour() {
        assertThat(TourOutline.of("java", "Java", List.of()).domains()).isEmpty();
    }

    private static TourEntry entry(String domain, String chapter, String notion) {
        return new TourEntry(notion, domain, "Domaine " + domain, chapter, "Chapitre " + chapter, "Page " + notion, 1);
    }
}
