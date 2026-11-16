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
        return films.findByRealisateurOrderByAnneeAsc(realisateur);
    }

    public List<Film> sortisEntre(int debut, int fin) {
        return films.findByAnneeBetween(debut, fin);
    }

    public List<Film> rechercher(String mot) {
        return films.findByTitreContainingIgnoreCase(mot);
    }

    public List<Film> lesTroisPlusLongs() {
        return films.findTop3ByOrderByDureeMinutesDesc();
    }

    public boolean existe(String titre) {
        return films.existsByTitre(titre);
    }

    public long nombreDe(String genre) {
        return films.countByGenre(genre);
    }
}
