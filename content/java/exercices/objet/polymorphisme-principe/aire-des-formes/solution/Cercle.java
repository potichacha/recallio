public record Cercle(double rayon) implements Forme {

    @Override
    public double aire() {
        return Math.PI * rayon * rayon;
    }
}
