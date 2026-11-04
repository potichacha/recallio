import java.util.Collection;
import java.util.function.Predicate;

public final class OutilsCollections {

    private OutilsCollections() {
    }

    public static double somme(Collection<? extends Number> nombres) {
        double total = 0;
        for (Number n : nombres) {
            total += n.doubleValue();
        }
        return total;
    }

    public static <T> void copierSi(Collection<? extends T> source, Predicate<? super T> condition,
            Collection<? super T> cible) {
        for (T element : source) {
            if (condition.test(element)) {
                cible.add(element);
            }
        }
    }

    public static <T> void remplir(Collection<? super T> cible, T valeur, int fois) {
        for (int i = 0; i < fois; i++) {
            cible.add(valeur);
        }
    }
}
