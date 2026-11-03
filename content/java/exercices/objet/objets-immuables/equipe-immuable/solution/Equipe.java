import java.util.ArrayList;
import java.util.List;

public record Equipe(String nom, List<String> membres) {

    public Equipe {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom obligatoire");
        }
        membres = List.copyOf(membres);
    }

    public Equipe avec(String membre) {
        List<String> nouveaux = new ArrayList<>(membres);
        nouveaux.add(membre);
        return new Equipe(nom, nouveaux);
    }
}
