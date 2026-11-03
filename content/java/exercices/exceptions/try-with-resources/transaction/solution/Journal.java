import java.util.ArrayList;
import java.util.List;

public class Journal {

    private final List<String> evenements = new ArrayList<>();

    public void noter(String evenement) {
        evenements.add(evenement);
    }

    public List<String> evenements() {
        return List.copyOf(evenements);
    }
}
