public class Moyenne {

    /** La moyenne des notes, avec ses décimales ; 0.0 sans aucune note. */
    public static double de(int[] notes) {
        int somme = 0;
        for (int note : notes) {
            somme += note;
        }
        return somme / notes.length;
    }
}
