import java.util.Locale;

public final class Textes {

    private static final int VISIBLES = 4;

    private Textes() {
    }

    public static String initiales(String nomComplet) {
        StringBuilder initiales = new StringBuilder();
        for (String mot : nomComplet.strip().split("\\s+")) {
            if (!mot.isEmpty()) {
                initiales.append(Character.toUpperCase(mot.charAt(0)));
            }
        }
        return initiales.toString();
    }

    public static boolean estPalindrome(String texte) {
        String normalise = texte.replace(" ", "").toLowerCase(Locale.ROOT);
        return normalise.equals(new StringBuilder(normalise).reverse().toString());
    }

    public static String masquer(String numero) {
        if (numero.length() <= VISIBLES) {
            return numero;
        }
        int caches = numero.length() - VISIBLES;
        return "*".repeat(caches) + numero.substring(caches);
    }
}
