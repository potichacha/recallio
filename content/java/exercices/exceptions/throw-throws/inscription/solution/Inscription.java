public final class Inscription {

    private static final int AGE_MINIMUM = 13;
    private static final int AGE_MAXIMUM = 120;

    private Inscription() {
    }

    public static void verifier(String email, int age) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email invalide : " + email);
        }
        if (age < AGE_MINIMUM) {
            throw new IllegalArgumentException("âge minimum " + AGE_MINIMUM + " ans : " + age);
        }
        if (age > AGE_MAXIMUM) {
            throw new IllegalArgumentException("âge invraisemblable : " + age);
        }
    }
}
