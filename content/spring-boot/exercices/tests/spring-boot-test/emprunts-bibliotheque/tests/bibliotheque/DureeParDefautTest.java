package bibliotheque;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Toute l'application telle qu'elle démarre en production : sans propriété, avec sa propre horloge.
@SpringBootTest
class DureeParDefautTest {

    @Autowired
    EmpruntService emprunts;

    @Autowired
    Clock horloge;

    @Test
    void lApplicationDeclareUneHorlogeEnUtc() {
        assertThat(horloge.getZone().getId()).isEqualTo("Z");
    }

    @Test
    void preteTroisSemainesQuandLaDureeNEstPasConfiguree() {
        Emprunt emprunt = emprunts.emprunter("9782070368228", "ada");

        assertThat(emprunt.retourPrevu()).isEqualTo(LocalDate.now(horloge).plusDays(21));
    }
}
