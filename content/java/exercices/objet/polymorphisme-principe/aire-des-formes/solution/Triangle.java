public record Triangle(double base, double hauteur) implements Forme {

    @Override
    public double aire() {
        return base * hauteur / 2;
    }
}
