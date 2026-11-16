package librairie;

// Expose le catalogue sous /api/livres : la liste, un livre par son ISBN, et l'ajout d'un livre.
public class LivreController {

    private final CatalogueService catalogue;

    public LivreController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }
}
