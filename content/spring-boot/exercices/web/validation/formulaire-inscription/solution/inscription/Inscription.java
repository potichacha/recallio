package inscription;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// La requête d'inscription, reçue en JSON.
public record Inscription(
        @NotBlank @Size(min = 3, max = 20) String pseudo,
        @NotBlank @Email String email,
        @Min(16) int age) {}
