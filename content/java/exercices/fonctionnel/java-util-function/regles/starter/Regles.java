import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class Regles {

    private Regles() {
    }

    public static <T> Predicate<T> toutes(List<Predicate<T>> regles) {
        return x -> false; // À écrire.
    }

    public static <T> Predicate<T> auMoinsUne(List<Predicate<T>> regles) {
        return x -> true; // À écrire.
    }

    public static UnaryOperator<String> enchainer(List<UnaryOperator<String>> etapes) {
        return s -> ""; // À écrire.
    }
}
