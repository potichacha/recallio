package bibliotheque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(Catalogue.class)
class CatalogueTest {

    @Autowired
    Catalogue catalogue;

    @Autowired
    LivreRepository livres;

    @Test
    void ajouterEnregistreLeLivreEtRenvoieSonIdentifiant() {
        Long id = catalogue.ajouter("Germinal", "Zola", 1885);

        assertThat(id).isNotNull();
        assertThat(livres.findById(id)).get().extracting(Livre::getTitre).isEqualTo("Germinal");
    }

    @Test
    void trouverRenvoieLeLivreEnregistre() {
        Long id = livres.save(new Livre("Nana", "Zola", 1880)).getId();

        assertThat(catalogue.trouver(id).getAuteur()).isEqualTo("Zola");
    }

    @Test
    void trouverUnLivreAbsentLeveLivreIntrouvable() {
        assertThatThrownBy(() -> catalogue.trouver(999L)).isInstanceOf(LivreIntrouvableException.class);
    }

    @Test
    void tousParTitreRangeLesLivresDansLOrdreAlphabetique() {
        livres.save(new Livre("Nana", "Zola", 1880));
        livres.save(new Livre("Germinal", "Zola", 1885));
        livres.save(new Livre("Bel-Ami", "Maupassant", 1885));

        assertThat(catalogue.tousParTitre()).extracting(Livre::getTitre)
                .containsExactly("Bel-Ami", "Germinal", "Nana");
    }

    @Test
    void retirerSupprimeLeLivre() {
        Long id = livres.save(new Livre("Nana", "Zola", 1880)).getId();

        catalogue.retirer(id);

        assertThat(livres.existsById(id)).isFalse();
    }

    @Test
    void retirerUnLivreAbsentLeveLivreIntrouvable() {
        assertThatThrownBy(() -> catalogue.retirer(999L)).isInstanceOf(LivreIntrouvableException.class);
    }

    @Test
    void nombreCompteLesLivres() {
        livres.save(new Livre("Nana", "Zola", 1880));
        livres.save(new Livre("Germinal", "Zola", 1885));

        assertThat(catalogue.nombre()).isEqualTo(2);
    }
}
