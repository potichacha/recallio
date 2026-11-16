package mediatheque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

// Seule la couche JPA démarre, sur une base H2 en mémoire ; chaque test est annulé à la fin (rollback).
@DataJpaTest
class LivreRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    LivreRepository livres;

    @BeforeEach
    void remplitLeCatalogue() {
        em.persist(new Livre("9782070360024", "L'Étranger", "Camus", 1942, true));
        em.persist(new Livre("9782070360536", "La Peste", "Camus", 1947, false));
        em.persist(new Livre("9782070368228", "Le Premier Homme", "Camus", 1994, true));
        em.persist(new Livre("9782070612758", "Le Petit Prince", "Saint-Exupéry", 1943, true));
        em.flush();
        em.clear();
    }

    @Test
    void trouveUnLivreParSonIsbn() {
        assertThat(livres.findByIsbn("9782070360536"))
                .hasValueSatisfying(livre -> assertThat(livre.getTitre()).isEqualTo("La Peste"));
    }

    @Test
    void renvoieUnOptionalVidePourUnIsbnInconnu() {
        assertThat(livres.findByIsbn("9780000000000")).isEmpty();
    }

    @Test
    void rangeLesLivresDUnAuteurDuPlusAncienAuPlusRecent() {
        assertThat(livres.findByAuteurOrderByAnneeAsc("Camus"))
                .extracting(Livre::getTitre)
                .containsExactly("L'Étranger", "La Peste", "Le Premier Homme");
    }

    @Test
    void compteLesLivresDisponibles() {
        assertThat(livres.countByDisponibleTrue()).isEqualTo(3);
    }

    @Test
    void chercheUnMotDansLeTitreSansTenirCompteDeLaCasse() {
        assertThat(livres.rechercherParTitre("PE"))
                .extracting(Livre::getTitre)
                .containsExactly("La Peste", "Le Petit Prince");
    }

    @Test
    void refuseDeuxLivresAvecLeMemeIsbn() {
        assertThatThrownBy(() -> livres.saveAndFlush(new Livre("9782070360024", "Copie", "Camus", 1942, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
