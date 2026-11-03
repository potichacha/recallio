public sealed interface Expr {

    record Nombre(int valeur) implements Expr {
    }

    record Somme(Expr gauche, Expr droite) implements Expr {
    }

    record Produit(Expr gauche, Expr droite) implements Expr {
    }

    record Negation(Expr operande) implements Expr {
    }
}
