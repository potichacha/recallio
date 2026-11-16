package bibliotheque;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class Catalogue {

    private final LivreRepository livres;

    public Catalogue(LivreRepository livres) {
        this.livres = livres;
    }

    public Long ajouter(String titre, String auteur, int annee) {
        return livres.save(new Livre(titre, auteur, annee)).getId();
    }

    public Livre trouver(Long id) {
        return livres.findById(id).orElseThrow(() -> new LivreIntrouvableException(id));
    }

    public List<Livre> tousParTitre() {
        return livres.findAll(Sort.by("titre"));
    }

    public void retirer(Long id) {
        if (!livres.existsById(id)) {
            throw new LivreIntrouvableException(id);
        }
        livres.deleteById(id);
    }

    public long nombre() {
        return livres.count();
    }
}
