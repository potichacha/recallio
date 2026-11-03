public class Duree {

    /** La durée sous la forme « 1 h 2 min 5 s ». */
    public static String enTexte(int secondes) {
        int heures = secondes / 3600;
        int minutes = secondes % 3600 / 60;
        int reste = secondes % 60;
        return heures + " h " + minutes + " min " + reste + " s";
    }
}
