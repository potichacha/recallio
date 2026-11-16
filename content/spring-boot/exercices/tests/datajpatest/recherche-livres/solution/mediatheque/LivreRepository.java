package mediatheque;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LivreRepository extends JpaRepository<Livre, Long> {

    Optional<Livre> findByIsbn(String isbn);

    List<Livre> findByAuteurOrderByAnneeAsc(String auteur);

    long countByDisponibleTrue();

    @Query("select l from Livre l where lower(l.titre) like lower(concat('%', :mot, '%')) order by l.titre")
    List<Livre> rechercherParTitre(@Param("mot") String mot);
}
