public final class Calcul {

    private Calcul() {
    }

    public static int evaluer(Expr expr) {
        return switch (expr) {
            case Expr.Nombre(int valeur) -> valeur;
            case Expr.Somme(Expr gauche, Expr droite) -> evaluer(gauche) + evaluer(droite);
            case Expr.Produit(Expr gauche, Expr droite) -> evaluer(gauche) * evaluer(droite);
            case Expr.Negation(Expr operande) -> -evaluer(operande);
        };
    }

    public static String afficher(Expr expr) {
        return switch (expr) {
            case Expr.Nombre(int valeur) -> String.valueOf(valeur);
            case Expr.Somme(Expr gauche, Expr droite) -> "(" + afficher(gauche) + " + " + afficher(droite) + ")";
            case Expr.Produit(Expr gauche, Expr droite) -> "(" + afficher(gauche) + " * " + afficher(droite) + ")";
            case Expr.Negation(Expr operande) -> "-" + afficher(operande);
        };
    }
}
