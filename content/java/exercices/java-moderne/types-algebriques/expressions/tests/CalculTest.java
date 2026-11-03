import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalculTest {

    private static Expr n(int valeur) {
        return new Expr.Nombre(valeur);
    }

    @Test
    void evalueUnNombre() {
        assertEquals(7, Calcul.evaluer(n(7)));
    }

    @Test
    void evalueUneExpressionImbriquee() {
        Expr e = new Expr.Somme(n(1), new Expr.Produit(n(2), new Expr.Negation(n(3))));

        assertEquals(-5, Calcul.evaluer(e));
    }

    @Test
    void evalueUneDoubleNegation() {
        assertEquals(4, Calcul.evaluer(new Expr.Negation(new Expr.Negation(n(4)))));
    }

    @Test
    void afficheUnNombreSansParentheses() {
        assertEquals("42", Calcul.afficher(n(42)));
    }

    @Test
    void afficheChaqueOperationEntreParentheses() {
        Expr e = new Expr.Somme(n(1), new Expr.Produit(n(2), new Expr.Negation(n(3))));

        assertEquals("(1 + (2 * -3))", Calcul.afficher(e));
    }
}
