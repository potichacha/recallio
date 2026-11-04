import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class Pile<T> {

    private final Deque<T> elements = new ArrayDeque<>();

    public void empiler(T element) {
        elements.push(element);
    }

    public T depiler() {
        if (elements.isEmpty()) {
            throw new IllegalStateException("pile vide");
        }
        return elements.pop();
    }

    public Optional<T> sommet() {
        return Optional.ofNullable(elements.peek());
    }

    public int taille() {
        return elements.size();
    }

    public boolean estVide() {
        return elements.isEmpty();
    }
}
