package banque;

// Les données échangées par l'API : un compte, un virement, et le corps d'une réponse d'erreur.
public final class Modeles {

    private Modeles() {}

    public record Compte(String numero, long soldeEnCentimes) {}

    public record Virement(String source, String destination, long montantEnCentimes) {}

    public record Erreur(String code, String message) {}
}
