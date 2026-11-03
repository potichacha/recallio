public final class Remise {

    private static final long SEUIL_PALIER_1 = 10_000;
    private static final long SEUIL_PALIER_2 = 50_000;
    private static final long PLAFOND = 10_000;
    private static final int BONUS_FIDELITE = 5;

    private Remise() {
    }

    public static long calculer(long montantCentimes, boolean clientFidele) {
        if (montantCentimes < 0) {
            throw new IllegalArgumentException("montant négatif : " + montantCentimes);
        }
        int taux = tauxDeBase(montantCentimes) + (clientFidele ? BONUS_FIDELITE : 0);
        long remise = montantCentimes * taux / 100;
        return Math.min(remise, PLAFOND);
    }

    private static int tauxDeBase(long montantCentimes) {
        if (montantCentimes >= SEUIL_PALIER_2) {
            return 10;
        }
        if (montantCentimes >= SEUIL_PALIER_1) {
            return 5;
        }
        return 0;
    }
}
