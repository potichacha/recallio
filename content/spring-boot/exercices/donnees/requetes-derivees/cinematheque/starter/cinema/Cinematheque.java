package cinema;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Cinematheque {

    private final FilmRepository films;

    public Cinematheque(FilmRepository films) {
        this.films = films;
    }

    public List<Film> filmsDe(String realisateur) {
        return List.of(); // À écrire.
    }

    public List<Film> sortisEntre(int debut, int fin) {
        return List.of(); // À écrire.
    }

    public List<Film> rechercher(String mot) {
        return List.of(); // À écrire.
    }

    public List<Film> lesTroisPlusLongs() {
        return List.of(); // À écrire.
    }

    public boolean existe(String titre) {
        return false; // À écrire.
    }

    public long nombreDe(String genre) {
        return 0; // À écrire.
    }
}
