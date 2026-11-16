package cinema;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmRepository extends JpaRepository<Film, Long> {

    // Déclare ici tes requêtes dérivées.
}
