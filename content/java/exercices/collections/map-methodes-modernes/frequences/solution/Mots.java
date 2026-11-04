import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class Mots {

    private Mots() {
    }

    public static Map<String, Integer> frequences(List<String> mots) {
        Map<String, Integer> frequences = new HashMap<>();
        for (String mot : mots) {
            frequences.merge(mot.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        return frequences;
    }

    public static Map<Character, List<String>> parInitiale(List<String> mots) {
        Map<Character, List<String>> groupes = new TreeMap<>();
        for (String mot : mots) {
            char initiale = Character.toLowerCase(mot.charAt(0));
            groupes.computeIfAbsent(initiale, k -> new ArrayList<>()).add(mot);
        }
        return groupes;
    }

    public static String plusFrequent(List<String> mots) {
        return frequences(mots).entrySet().stream()
                .min(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse("");
    }
}
