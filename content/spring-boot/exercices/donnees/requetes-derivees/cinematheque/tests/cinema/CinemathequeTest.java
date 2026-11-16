package cinema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(Cinematheque.class)
class CinemathequeTest {

    @Autowired
    Cinematheque cinematheque;

    @Autowired
    FilmRepository films;

    @BeforeEach
    void programmerLesFilms() {
        films.save(new Film("Sans toit ni loi", "Varda", 1985, 105, "drame"));
        films.save(new Film("Cléo de 5 à 7", "Varda", 1962, 90, "drame"));
        films.save(new Film("Le Bonheur", "Varda", 1965, 79, "drame"));
        films.save(new Film("Playtime", "Tati", 1967, 124, "comédie"));
        films.save(new Film("Mon oncle", "Tati", 1958, 116, "comédie"));
        films.save(new Film("Les Vacances de M. Hulot", "Tati", 1953, 114, "comédie"));
        films.save(new Film("La Jetée", "Marker", 1962, 28, "science-fiction"));
    }

    @Test
    void filmsDeRenvoieLesFilmsDuRealisateurDuPlusAncienAuPlusRecent() {
        assertThat(cinematheque.filmsDe("Varda")).extracting(Film::getTitre)
                .containsExactly("Cléo de 5 à 7", "Le Bonheur", "Sans toit ni loi");
    }

    @Test
    void sortisEntreIncluentLesDeuxBornes() {
        assertThat(cinematheque.sortisEntre(1958, 1965)).extracting(Film::getTitre)
                .containsExactlyInAnyOrder("Mon oncle", "Cléo de 5 à 7", "La Jetée", "Le Bonheur");
    }

    @Test
    void rechercherTrouveUnMotDuTitreSansTenirCompteDeLaCasse() {
        assertThat(cinematheque.rechercher("ONCLE")).extracting(Film::getTitre).containsExactly("Mon oncle");
        assertThat(cinematheque.rechercher("vacances")).extracting(Film::getTitre)
                .containsExactly("Les Vacances de M. Hulot");
    }

    @Test
    void lesTroisPlusLongsVontDuPlusLongAuPlusCourt() {
        assertThat(cinematheque.lesTroisPlusLongs()).extracting(Film::getTitre)
                .containsExactly("Playtime", "Mon oncle", "Les Vacances de M. Hulot");
    }

    @Test
    void existeSeulementPourUnTitreConnu() {
        assertThat(cinematheque.existe("Playtime")).isTrue();
        assertThat(cinematheque.existe("Trafic")).isFalse();
    }

    @Test
    void nombreDeCompteLesFilmsDuGenre() {
        assertThat(cinematheque.nombreDe("comédie")).isEqualTo(3);
        assertThat(cinematheque.nombreDe("western")).isZero();
    }
}
