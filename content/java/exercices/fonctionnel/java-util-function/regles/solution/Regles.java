import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class Regles {

    private Regles() {
    }

    public static <T> Predicate<T> toutes(List<Predicate<T>> regles) {
        return regles.stream().reduce(x -> true, Predicate::and);
    }

    public static <T> Predicate<T> auMoinsUne(List<Predicate<T>> regles) {
        return regles.stream().reduce(x -> false, Predicate::or);
    }

    public static UnaryOperator<String> enchainer(List<UnaryOperator<String>> etapes) {
        return etapes.stream()
                .reduce(UnaryOperator.identity(), (avant, apres) -> avant.andThen(apres)::apply);
    }
}
