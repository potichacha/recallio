public enum Operation {
    PLUS("+"),
    MOINS("-"),
    FOIS("*"),
    DIVISE("/");

    private final String symbole;

    Operation(String symbole) {
        this.symbole = symbole;
    }

    public String symbole() {
        return symbole;
    }

    public int appliquer(int a, int b) {
        return 0; // À écrire.
    }

    public static Operation deSymbole(String symbole) {
        return PLUS; // À écrire.
    }
}
