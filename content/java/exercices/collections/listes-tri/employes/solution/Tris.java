import java.util.Comparator;
import java.util.List;

public final class Tris {

    private Tris() {
    }

    public static List<Employe> parServicePuisSalaireDecroissant(List<Employe> employes) {
        return employes.stream()
                .sorted(Comparator.comparing(Employe::service)
                        .thenComparing(Employe::salaire, Comparator.reverseOrder()))
                .toList();
    }

    public static List<Employe> parNomSansCasse(List<Employe> employes) {
        return employes.stream()
                .sorted(Comparator.comparing(Employe::nom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static List<Employe> mieuxPayes(List<Employe> employes, int n) {
        return employes.stream()
                .sorted(Comparator.comparingInt(Employe::salaire).reversed())
                .limit(n)
                .toList();
    }
}
