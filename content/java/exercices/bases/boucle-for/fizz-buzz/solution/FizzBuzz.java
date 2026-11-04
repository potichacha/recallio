public class FizzBuzz {

    /** Les nombres de 1 à n, séparés par des espaces, avec Fizz, Buzz et FizzBuzz. */
    public static String jusqua(int n) {
        StringBuilder resultat = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) {
                resultat.append(' ');
            }
            if (i % 15 == 0) {
                resultat.append("FizzBuzz");
            } else if (i % 3 == 0) {
                resultat.append("Fizz");
            } else if (i % 5 == 0) {
                resultat.append("Buzz");
            } else {
                resultat.append(i);
            }
        }
        return resultat.toString();
    }
}
