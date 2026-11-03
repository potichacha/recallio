import java.util.Collection;
import java.util.function.Predicate;

public final class OutilsCollections {

    private OutilsCollections() {
    }

    public static double somme(Collection<? extends Number> nombres) {
        return 0; // À écrire.
    }

    public static <T> void copierSi(Collection<? extends T> source, Predicate<? super T> condition,
            Collection<? super T> cible) {
        // À écrire.
    }

    public static <T> void remplir(Collection<? super T> cible, T valeur, int fois) {
        // À écrire.
    }
}
