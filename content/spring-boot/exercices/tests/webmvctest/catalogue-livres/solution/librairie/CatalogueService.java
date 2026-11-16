package librairie;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService {

    private final Map<String, Livre> livres = new ConcurrentHashMap<>();

    public Optional<Livre> trouver(String isbn) {
        return Optional.ofNullable(livres.get(isbn));
    }

    public void ajouter(Livre livre) {
        livres.put(livre.isbn(), livre);
    }
}
