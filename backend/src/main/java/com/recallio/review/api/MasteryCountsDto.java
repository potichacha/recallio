package com.recallio.review.api;

import com.recallio.review.domain.track.MasteryCounts;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Les notions par niveau de maîtrise (ADR 0034), pour la carte du parcours et le tableau de bord. */
@Schema(
        name = "MasteryCounts",
        description =
                "Les notions par niveau de maîtrise. Chaque compte inclut les niveaux supérieurs : une notion experte"
                        + " compte aussi comme maîtrisée, acquise et découverte.")
public record MasteryCountsDto(
        @Schema(description = "Toutes les notions, nouvelles comprises")
        int total,

        @Schema(description = "Les notions au moins découvertes")
        int discovered,

        @Schema(description = "Les notions au moins acquises")
        int acquired,

        @Schema(description = "Les notions au moins maîtrisées")
        int mastered,

        @Schema(description = "Les notions expertes") int expert) {

    static MasteryCountsDto from(MasteryCounts counts) {
        return new MasteryCountsDto(
                counts.total(), counts.discovered(), counts.acquired(), counts.mastered(), counts.expert());
    }
}
