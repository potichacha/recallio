import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Ensembles {

    private Ensembles() {
    }

    public static <T> Set<T> communs(Set<T> a, Set<T> b) {
        Set<T> resultat = new HashSet<>(a);
        resultat.retainAll(b);
        return resultat;
    }

    public static <T> Set<T> differenceSymetrique(Set<T> a, Set<T> b) {
        Set<T> resultat = new HashSet<>(a);
        resultat.addAll(b);
        resultat.removeAll(communs(a, b));
        return resultat;
    }

    public static <T> Set<T> doublons(List<T> liste) {
        Set<T> vus = new HashSet<>();
        Set<T> doublons = new LinkedHashSet<>();
        for (T element : liste) {
            if (!vus.add(element)) {
                doublons.add(element);
            }
        }
        return doublons;
    }
}
