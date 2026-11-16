package librairie;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livres")
public class LivreController {

    private final CatalogueService catalogue;

    public LivreController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<Livre> lire(@PathVariable String isbn) {
        return ResponseEntity.of(catalogue.trouver(isbn));
    }

    @PostMapping
    public ResponseEntity<Livre> ajouter(@Valid @RequestBody Livre livre) {
        catalogue.ajouter(livre);
        return ResponseEntity.created(URI.create("/api/livres/" + livre.isbn())).body(livre);
    }
}
