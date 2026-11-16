package boutique;

/** Le corps des réponses d'erreur : un code stable, et un message pour un humain. */
public record Erreur(String code, String message) {}
