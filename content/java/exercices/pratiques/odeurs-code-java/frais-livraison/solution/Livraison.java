public final class Livraison {

    private static final long SEUIL_GRATUITE_PARTICULIER = 5_000;
    private static final long FRAIS_PARTICULIER = 490;
    private static final long FRAIS_INTERNATIONAL = 990;
    private static final long SUPPLEMENT_EXPRESS = 1_000;

    private Livraison() {
    }

    public static long frais(Commande commande) {
        long supplement = commande.express() ? SUPPLEMENT_EXPRESS : 0;
        return fraisStandard(commande) + supplement;
    }

    private static long fraisStandard(Commande commande) {
        return switch (commande.typeClient()) {
            case PARTICULIER -> commande.montantCentimes() < SEUIL_GRATUITE_PARTICULIER ? FRAIS_PARTICULIER : 0;
            case PROFESSIONNEL -> 0;
            case INTERNATIONAL -> FRAIS_INTERNATIONAL;
        };
    }
}
