package cinema;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmRepository extends JpaRepository<Film, Long> {

    List<Film> findByRealisateurOrderByAnneeAsc(String realisateur);

    List<Film> findByAnneeBetween(int debut, int fin);

    List<Film> findByTitreContainingIgnoreCase(String mot);

    List<Film> findTop3ByOrderByDureeMinutesDesc();

    boolean existsByTitre(String titre);

    long countByGenre(String genre);
}
