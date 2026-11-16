package bibliotheque;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Catalogue {

    private final LivreRepository livres;

    public Catalogue(LivreRepository livres) {
        this.livres = livres;
    }

    public Long ajouter(String titre, String auteur, int annee) {
        return null; // À écrire.
    }

    public Livre trouver(Long id) {
        return null; // À écrire.
    }

    public List<Livre> tousParTitre() {
        return List.of(); // À écrire.
    }

    public void retirer(Long id) {
        // À écrire.
    }

    public long nombre() {
        return 0; // À écrire.
    }
}
