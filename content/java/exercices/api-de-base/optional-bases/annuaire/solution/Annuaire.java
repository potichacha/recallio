import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Annuaire {

    private final Map<String, String> adresses;

    public Annuaire(Map<String, String> adresses) {
        this.adresses = Map.copyOf(adresses);
    }

    public Optional<String> email(String nom) {
        return Optional.ofNullable(adresses.get(nom));
    }

    public String domaine(String nom) {
        return email(nom)
                .map(e -> e.substring(e.indexOf('@') + 1))
                .orElse("inconnu");
    }

    public List<String> emailsConnus(List<String> noms) {
        return noms.stream()
                .map(this::email)
                .flatMap(Optional::stream)
                .toList();
    }
}
