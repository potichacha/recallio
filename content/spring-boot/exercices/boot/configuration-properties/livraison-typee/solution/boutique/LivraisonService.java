package boutique;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class LivraisonService {

    private final LivraisonProperties livraison;

    public LivraisonService(LivraisonProperties livraison) {
        this.livraison = livraison;
    }

    public BigDecimal frais(BigDecimal montantPanier) {
        return montantPanier.compareTo(livraison.seuilGratuite()) >= 0 ? BigDecimal.ZERO : livraison.frais();
    }

    public LocalDate dateDeLivraison(LocalDate dateDeCommande) {
        return dateDeCommande.plusDays(livraison.delai().toDays());
    }
}
