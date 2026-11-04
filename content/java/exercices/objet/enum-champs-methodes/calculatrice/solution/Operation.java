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
        return switch (this) {
            case PLUS -> a + b;
            case MOINS -> a - b;
            case FOIS -> a * b;
            case DIVISE -> a / b;
        };
    }

    public static Operation deSymbole(String symbole) {
        for (Operation operation : values()) {
            if (operation.symbole.equals(symbole)) {
                return operation;
            }
        }
        throw new IllegalArgumentException("symbole inconnu : " + symbole);
    }
}
