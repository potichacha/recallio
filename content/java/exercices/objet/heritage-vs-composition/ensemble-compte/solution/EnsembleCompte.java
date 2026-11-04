import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EnsembleCompte {

    private final Set<String> elements = new HashSet<>();
    private int ajouts;

    public boolean ajouter(String e) {
        ajouts++;
        return elements.add(e);
    }

    public void ajouterTous(Collection<String> nouveaux) {
        ajouts += nouveaux.size();
        elements.addAll(nouveaux);
    }

    public boolean contient(String e) {
        return elements.contains(e);
    }

    public int taille() {
        return elements.size();
    }

    public int nombreAjouts() {
        return ajouts;
    }
}
