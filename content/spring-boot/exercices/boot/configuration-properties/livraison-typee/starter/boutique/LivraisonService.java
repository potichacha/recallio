package boutique;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LivraisonService {

    @Value("${boutique.livraison.frais}")
    private BigDecimal frais;

    @Value("${boutique.livraison.seuil-gratuite}")
    private BigDecimal seuilGratuite;

    @Value("${boutique.livraison.delai:2d}")
    private Duration delai;

    public BigDecimal frais(BigDecimal montantPanier) {
        return montantPanier.compareTo(seuilGratuite) >= 0 ? BigDecimal.ZERO : frais;
    }

    public LocalDate dateDeLivraison(LocalDate dateDeCommande) {
        return dateDeCommande.plusDays(delai.toDays());
    }
}
