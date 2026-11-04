public final class Saisies {

    private Saisies() {
    }

    public static int entierOuDefaut(String texte, int defaut) {
        if (texte == null) {
            return defaut;
        }
        try {
            return Integer.parseInt(texte.strip());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    public static int somme(String liste) {
        int total = 0;
        for (String element : liste.split(",")) {
            total += entierOuDefaut(element, 0);
        }
        return total;
    }
}
