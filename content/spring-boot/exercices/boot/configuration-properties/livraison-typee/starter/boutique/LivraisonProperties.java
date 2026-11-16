package boutique;

import java.math.BigDecimal;
import java.time.Duration;

// À lier au préfixe boutique.livraison, et à valider.
public record LivraisonProperties(BigDecimal frais, BigDecimal seuilGratuite, Duration delai) {
}
