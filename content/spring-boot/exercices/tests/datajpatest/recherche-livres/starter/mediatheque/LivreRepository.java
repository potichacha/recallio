package mediatheque;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivreRepository extends JpaRepository<Livre, Long> {

    default Optional<Livre> findByIsbn(String isbn) {
        throw new UnsupportedOperationException("à écrire");
    }

    default List<Livre> findByAuteurOrderByAnneeAsc(String auteur) {
        throw new UnsupportedOperationException("à écrire");
    }

    default long countByDisponibleTrue() {
        throw new UnsupportedOperationException("à écrire");
    }

    default List<Livre> rechercherParTitre(String mot) {
        throw new UnsupportedOperationException("à écrire");
    }
}
