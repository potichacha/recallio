import java.util.LinkedHashSet;
import java.util.List;

public final class Doublons {

    private Doublons() {}

    /** Les mots sans doublon, dans l'ordre de leur première apparition. */
    public static List<String> sansDoublons(List<String> mots) {
        return List.copyOf(new LinkedHashSet<>(mots));
    }
}
