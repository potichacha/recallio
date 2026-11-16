package librairie;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Expose le catalogue sous /api/livres : la liste, un livre par son ISBN, et l'ajout d'un livre.
@RestController
@RequestMapping("/api/livres")
public class LivreController {

    private final CatalogueService catalogue;

    public LivreController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<Livre> tous() {
        return catalogue.tous();
    }

    @GetMapping("/{isbn}")
    public Livre unLivre(@PathVariable String isbn) {
        return catalogue.trouver(isbn);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Livre ajouter(@RequestBody Livre livre) {
        return catalogue.ajouter(livre);
    }
}
