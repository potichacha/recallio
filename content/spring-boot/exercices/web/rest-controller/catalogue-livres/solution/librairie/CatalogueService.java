package librairie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService {

    private final Map<String, Livre> livres = new LinkedHashMap<>();

    public synchronized List<Livre> tous() {
        return new ArrayList<>(livres.values());
    }

    public synchronized Livre trouver(String isbn) {
        Livre livre = livres.get(isbn);
        if (livre == null) {
            throw new LivreIntrouvableException(isbn);
        }
        return livre;
    }

    public synchronized Livre ajouter(Livre livre) {
        livres.put(livre.isbn(), livre);
        return livre;
    }
}
