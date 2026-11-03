import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toCollection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Rapports {

    private Rapports() {
    }

    public static Map<String, Integer> totalParRegion(List<Vente> ventes) {
        return ventes.stream()
                .collect(groupingBy(Vente::region, TreeMap::new, summingInt(Vente::montant)));
    }

    public static Map<String, Set<String>> vendeursParRegion(List<Vente> ventes) {
        return ventes.stream()
                .collect(groupingBy(Vente::region, TreeMap::new,
                        mapping(Vente::vendeur, toCollection(TreeSet::new))));
    }

    public static Map<Boolean, List<Vente>> grossesVentes(List<Vente> ventes, int seuil) {
        return ventes.stream().collect(partitioningBy(v -> v.montant() >= seuil));
    }
}
