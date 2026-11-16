package bibliotheque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Toute l'application, avec une durée d'emprunt de 14 jours et une horloge arrêtée au lundi 2 mars 2026.
// Les tests partagent le même contexte, donc le même service : chacun emprunte un livre différent.
@SpringBootTest(properties = "bibliotheque.duree-emprunt=14d")
class EmpruntServiceTest {

    @TestConfiguration
    static class HorlogeDeTest {

        @Bean
        @Primary
        Clock horlogeDeTest() {
            return Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    EmpruntService emprunts;

    @Test
    void fixeLeRetourSelonLaDureeConfiguree() {
        Emprunt emprunt = emprunts.emprunter("9782070360024", "ada");

        assertThat(emprunt.retourPrevu()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    void noteLeLivreEtLeLecteur() {
        Emprunt emprunt = emprunts.emprunter("9782070360536", "grace");

        assertThat(emprunt.isbn()).isEqualTo("9782070360536");
        assertThat(emprunt.lecteur()).isEqualTo("grace");
    }

    @Test
    void refuseUnLivreDejaEmprunte() {
        emprunts.emprunter("9782253006329", "ada");

        assertThatThrownBy(() -> emprunts.emprunter("9782253006329", "linus"))
                .isInstanceOf(LivreIndisponibleException.class);
    }

    @Test
    void unLivreRenduPeutEtreEmprunteDeNouveau() {
        emprunts.emprunter("9782070413119", "ada");
        emprunts.rendre("9782070413119");

        assertThat(emprunts.emprunter("9782070413119", "linus").lecteur()).isEqualTo("linus");
    }
}
