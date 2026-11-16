package boutique;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "boutique.livraison.frais=4.90",
        "boutique.livraison.seuil-gratuite=50",
        "boutique.livraison.delai=3d"
})
class LivraisonServiceTest {

    @Autowired
    LivraisonService livraison;

    @Autowired
    LivraisonProperties proprietes;

    @Test
    void lieLesProprietesDuPrefixeBoutiqueLivraison() {
        assertThat(proprietes.frais()).isEqualByComparingTo("4.90");
        assertThat(proprietes.seuilGratuite()).isEqualByComparingTo("50");
        assertThat(proprietes.delai()).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void factureLesFraisSousLeSeuil() {
        assertThat(livraison.frais(new BigDecimal("20"))).isEqualByComparingTo("4.90");
    }

    @Test
    void offreLaLivraisonAPartirDuSeuil() {
        assertThat(livraison.frais(new BigDecimal("50"))).isEqualByComparingTo("0");
    }

    @Test
    void calculeLaDateDeLivraisonAvecLeDelaiConfigure() {
        assertThat(livraison.dateDeLivraison(LocalDate.of(2026, 3, 2))).isEqualTo(LocalDate.of(2026, 3, 5));
    }
}
