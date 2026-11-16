package banque;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class Journal {

    private final List<String> entrees = new CopyOnWriteArrayList<>();

    public void ecrire(String entree) {
        entrees.add(entree);
    }

    public List<String> entrees() {
        return List.copyOf(entrees);
    }

    public void vider() {
        entrees.clear();
    }
}
