public record Rectangle(double largeur, double hauteur) implements Forme {

    @Override
    public double aire() {
        return largeur * hauteur;
    }
}
