public class Moyenne {

    /** La moyenne des notes, avec ses décimales ; 0.0 sans aucune note. */
    public static double de(int[] notes) {
        if (notes.length == 0) {
            return 0.0;
        }
        int somme = 0;
        for (int note : notes) {
            somme += note;
        }
        return (double) somme / notes.length;
    }
}
