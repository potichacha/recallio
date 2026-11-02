import java.util.List;

public final class Caisse {

    private Caisse() {}

    /** La somme, en centimes, des montants des commandes payées. */
    public static int totalPaye(List<Commande> commandes) {
        return commandes.stream()
                .filter(Commande::payee)
                .mapToInt(Commande::montantEnCentimes)
                .sum();
    }
}
