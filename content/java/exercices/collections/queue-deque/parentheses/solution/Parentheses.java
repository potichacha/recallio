import java.util.ArrayDeque;
import java.util.Deque;

public final class Parentheses {

    private Parentheses() {
    }

    public static boolean equilibree(String texte) {
        Deque<Character> attendus = new ArrayDeque<>();
        for (char c : texte.toCharArray()) {
            switch (c) {
                case '(' -> attendus.push(')');
                case '[' -> attendus.push(']');
                case '{' -> attendus.push('}');
                case ')', ']', '}' -> {
                    if (attendus.isEmpty() || attendus.pop() != c) {
                        return false;
                    }
                }
                default -> { }
            }
        }
        return attendus.isEmpty();
    }
}
