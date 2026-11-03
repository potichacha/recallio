import java.util.List;

public final class Formes {

    private Formes() {
    }

    public static double aireTotale(List<Forme> formes) {
        double total = 0;
        for (Forme forme : formes) {
            total += forme.aire();
        }
        return total;
    }
}
