package librairie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Livre(
        @NotBlank @Pattern(regexp = "\\d{13}") String isbn,
        @NotBlank String titre,
        @NotBlank String auteur) {
}
