public class Tableaux {

    /** Inverse l'ordre des éléments du tableau, en place. */
    public static void inverser(int[] t) {
        for (int i = 0; i < t.length / 2; i++) {
            int miroir = t.length - 1 - i;
            int tmp = t[i];
            t[i] = t[miroir];
            t[miroir] = tmp;
        }
    }
}
