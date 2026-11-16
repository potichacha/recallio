package bibliotheque;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record NouveauLivre(@NotBlank String titre, @NotBlank String auteur, @Min(1450) int annee) {}
