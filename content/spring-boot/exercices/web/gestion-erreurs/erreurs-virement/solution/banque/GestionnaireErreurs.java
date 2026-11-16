package banque;

import banque.Modeles.Erreur;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduis ici les exceptions métier en réponses HTTP, pour tous les contrôleurs : voir l'énoncé.
@RestControllerAdvice
public class GestionnaireErreurs {

    @ExceptionHandler(CompteIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Erreur compteIntrouvable(CompteIntrouvableException e) {
        return new Erreur("COMPTE_INTROUVABLE", e.getMessage());
    }

    @ExceptionHandler(SoldeInsuffisantException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Erreur soldeInsuffisant(SoldeInsuffisantException e) {
        return new Erreur("SOLDE_INSUFFISANT", e.getMessage());
    }
}
