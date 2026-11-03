public class Salutation {

    /** La phrase d'accueil pour ce prénom, ou une salutation générale sans prénom. */
    public static String saluer(String prenom) {
        return "";
    }

    public static void main(String[] args) {
        String prenom = args.length > 0 ? args[0] : null;
        System.out.println(saluer(prenom));
    }
}
