package bibliotheque;

import java.time.LocalDate;

public record Emprunt(String isbn, String lecteur, LocalDate retourPrevu) {
}
