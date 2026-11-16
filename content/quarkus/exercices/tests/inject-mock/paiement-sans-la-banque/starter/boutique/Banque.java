package boutique;

import jakarta.inject.Singleton;

/** Le client de la banque : en vrai, il appellerait son API. Les tests le remplacent par un mock. */
@Singleton
public class Banque {

    /** Débite la carte et renvoie l'identifiant de la transaction, ou lève PaiementRefuseException. */
    public String debiter(String carte, int montant) {
        throw new IllegalStateException("La banque n'est pas joignable depuis les tests");
    }
}
