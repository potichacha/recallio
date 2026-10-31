package com.recallio.content.infrastructure.markdown;

import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.InvalidSourceException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * L'en-tête d'un document de référence (ADR 0013) : quelques lignes {@code clé: valeur} entre deux lignes {@code ---},
 * tout en haut du fichier.
 *
 * <pre>
 * ---
 * subject: go
 * title: Go
 * category: languages
 * position: 20
 * status: published
 * ---
 * </pre>
 *
 * <p>Volontairement minimal (pas de YAML complet, donc aucune dépendance) : une valeur tient sur une ligne.
 */
final class SubjectHeader {

    private static final String DELIMITER = "---";
    private static final Set<String> KEYS = Set.of("subject", "title", "category", "position", "status");

    private SubjectHeader() {}

    /** Le sujet décrit par l'en-tête, et le nombre de lignes qu'il occupe (délimiteurs compris). */
    record Read(Subject subject, int lineCount) {}

    static Read read(String[] lines) {
        if (lines.length == 0 || !lines[0].strip().equals(DELIMITER)) {
            throw invalid("le document doit commencer par un en-tête « --- » qui décrit son sujet");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index].strip();
            if (line.equals(DELIMITER)) {
                return new Read(toSubject(values), index + 1);
            }
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            String key = colon < 0 ? line : line.substring(0, colon).strip();
            if (colon < 0 || !KEYS.contains(key)) {
                throw invalid("ligne %d de l'en-tête non reconnue : « %s » (clés attendues : %s)"
                        .formatted(index + 1, line, String.join(", ", KEYS)));
            }
            values.put(key, line.substring(colon + 1).strip());
        }
        throw invalid("l'en-tête n'est pas refermé par une ligne « --- »");
    }

    private static Subject toSubject(Map<String, String> values) {
        try {
            return new Subject(
                    required(values, "subject"),
                    required(values, "title"),
                    enumValue(Subject.Category.class, required(values, "category")),
                    Integer.parseInt(required(values, "position")),
                    enumValue(Subject.Status.class, required(values, "status")));
        } catch (IllegalArgumentException e) {
            throw invalid(e.getMessage() == null ? "valeur invalide" : e.getMessage());
        }
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("la clé « " + key + " » est obligatoire");
        }
        return value;
    }

    /** {@code cloud-devops} → {@code CLOUD_DEVOPS}. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("valeur inconnue « " + value + " » pour " + type.getSimpleName(), e);
        }
    }

    private static InvalidSourceException invalid(String reason) {
        return new InvalidSourceException("En-tête du document invalide : " + reason + ".");
    }
}
