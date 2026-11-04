import java.util.Collection;
import java.util.HashSet;

public class EnsembleCompte extends HashSet<String> {

    private int ajouts;

    @Override
    public boolean add(String e) {
        ajouts++;
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends String> elements) {
        ajouts += elements.size();
        return super.addAll(elements);
    }

    public boolean ajouter(String e) {
        return add(e);
    }

    public void ajouterTous(Collection<String> elements) {
        addAll(elements);
    }

    public boolean contient(String e) {
        return contains(e);
    }

    public int taille() {
        return size();
    }

    public int nombreAjouts() {
        return ajouts;
    }
}
