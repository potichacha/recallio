package boutique;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NouveauProduit(
        @NotBlank @Size(max = 50) String nom,
        @Positive int prix,
        @NotNull @Pattern(regexp = "[A-Z]{3}-\\d{3}") String reference) {}
