package boutique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommandeServiceTest {

    @Autowired
    CommandeService commandes;

    @Autowired
    Stock stock;

    @Autowired
    Journal journal;

    @Test
    void retireLaQuantiteDuStockPartage() {
        commandes.commander("cafe", 2);

        assertThat(stock.quantite("cafe")).isEqualTo(8);
    }

    @Test
    void noteLaCommandeDansLeJournalPartage() {
        commandes.commander("the", 1);

        assertThat(journal.messages()).contains("commande de 1 the");
    }

    @Test
    void refuseUneCommandeSuperieureAuStockSansRienRetirer() {
        assertThatThrownBy(() -> commandes.commander("chocolat", 4)).isInstanceOf(IllegalArgumentException.class);

        assertThat(stock.quantite("chocolat")).isEqualTo(3);
    }

    @Test
    void recoitSesDependancesParSonUniqueConstructeur() {
        Constructor<?>[] constructeurs = CommandeService.class.getDeclaredConstructors();

        assertThat(constructeurs).hasSize(1);
        assertThat(constructeurs[0].getParameterTypes()).containsExactlyInAnyOrder(Stock.class, Journal.class);
        for (Field champ : CommandeService.class.getDeclaredFields()) {
            assertThat(Modifier.isFinal(champ.getModifiers())).as("le champ %s est final", champ.getName()).isTrue();
        }
    }
}
