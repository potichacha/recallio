import java.util.List;

public record Equipe(String nom, List<String> membres) {

    public Equipe avec(String membre) {
        membres.add(membre);
        return this;
    }
}
