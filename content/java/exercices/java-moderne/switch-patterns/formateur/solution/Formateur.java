import java.util.List;
import java.util.stream.Collectors;

public final class Formateur {

    private Formateur() {
    }

    public static String formater(Object valeur) {
        return switch (valeur) {
            case null -> "-";
            case Integer i when i < 0 -> "(" + -i + ")";
            case Integer i -> String.valueOf(i);
            case String s when s.isBlank() -> "-";
            case String s -> s.strip();
            case Boolean b -> b ? "oui" : "non";
            case List<?> liste -> liste.stream()
                    .map(Formateur::formater)
                    .collect(Collectors.joining(", ", "[", "]"));
            default -> valeur.toString();
        };
    }
}
