import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Annuaire {

    private final Map<String, String> adresses;

    public Annuaire(Map<String, String> adresses) {
        this.adresses = Map.copyOf(adresses);
    }

    public Optional<String> email(String nom) {
        return Optional.of(adresses.get(nom));
    }

    public String domaine(String nom) {
        return ""; // À écrire.
    }

    public List<String> emailsConnus(List<String> noms) {
        return List.of(); // À écrire.
    }
}
