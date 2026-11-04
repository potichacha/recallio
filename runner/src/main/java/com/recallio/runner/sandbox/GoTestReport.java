package com.recallio.runner.sandbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Lit le rapport des tests Go produit dans le bac à sable (ADR 0033) : le flux de {@code test2json}, un événement JSON
 * par ligne ({@code run}, {@code output}, {@code pass}, {@code fail}...).
 *
 * <p>Un test est un test de plus haut niveau ou un sous-test sans sous-test : un parent dont un sous-test échoue échoue
 * aussi, et ne doit pas compter deux fois. Le message d'un test raté est ce qu'il a écrit ({@code t.Errorf}, une
 * {@code panic}), sans les lignes de cadre ({@code === RUN}, {@code --- FAIL}) ni la pile d'appels.
 */
final class GoTestReport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> RESULTS = Set.of("pass", "fail", "skip");
    private static final String FAIL = "fail";
    private static final String OUTPUT = "output";
    /** Les lignes que {@code go test} écrit lui-même autour de chaque test. */
    private static final String FRAME = "frame";
    /** Le début de la pile d'appels d'une {@code panic} : le message s'arrête avant elle. */
    private static final String STACK = "goroutine ";

    private GoTestReport() {}

    /**
     * Un événement de {@code test2json} ; seuls les champs utiles sont lus.
     *
     * @param test le nom du test ({@code TestSomme}, {@code TestSomme/vide}), absent pour le paquet entier
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Event(
            @JsonProperty("Action") String action,
            @JsonProperty("Test") @Nullable String test,
            @JsonProperty("Output") @Nullable String output,
            @JsonProperty("OutputType") @Nullable String outputType) {}

    /**
     * Les tests du rapport. Un rapport qui ne finit pas par le résultat du paquet vient d'un programme arrêté en route :
     * une {@code panic} y laisse un test raté, qu'on garde avec son message ; sans test raté, {@code test2json} a compté
     * comme réussi le test en cours, et le rapport ne donne aucun test.
     *
     * @throws IllegalArgumentException si une ligne n'est pas un événement lisible
     */
    static List<TestOutcome> parse(String report) {
        Map<String, Boolean> passed = new LinkedHashMap<>();
        Map<String, StringBuilder> written = new LinkedHashMap<>();
        boolean finished = false;
        for (String line : report.lines().filter(line -> !line.isBlank()).toList()) {
            Event event = event(line);
            String test = event.test();
            if (test == null) {
                finished |= RESULTS.contains(event.action());
                continue;
            }
            if (RESULTS.contains(event.action())) {
                passed.put(test, !event.action().equals(FAIL));
            } else if (event.action().equals(OUTPUT) && event.output() != null && !FRAME.equals(event.outputType())) {
                written.computeIfAbsent(test, _ -> new StringBuilder()).append(event.output());
            }
        }
        List<TestOutcome> outcomes = new ArrayList<>();
        if (!finished && !passed.containsValue(false)) {
            return outcomes;
        }
        passed.forEach((test, ok) -> {
            if (!hasSubtests(test, passed.keySet())) {
                outcomes.add(new TestOutcome(topLevel(test), test, ok, ok ? "" : message(written.get(test))));
            }
        });
        return outcomes;
    }

    private static Event event(String line) {
        try {
            return JSON.readValue(line, Event.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Rapport des tests Go illisible : " + e.getOriginalMessage(), e);
        }
    }

    private static boolean hasSubtests(String test, Set<String> tests) {
        return tests.stream().anyMatch(other -> other.startsWith(test + "/"));
    }

    private static String topLevel(String test) {
        int slash = test.indexOf('/');
        return slash < 0 ? test : test.substring(0, slash);
    }

    private static String message(@Nullable StringBuilder written) {
        String message = written == null
                ? ""
                : written.toString()
                        .lines()
                        .takeWhile(line -> !line.startsWith(STACK))
                        .map(String::strip)
                        .collect(Collectors.joining("\n"))
                        .strip();
        if (message.isEmpty()) {
            return "Le test a échoué.";
        }
        return message.length() <= JunitReport.MAX_MESSAGE_LENGTH
                ? message
                : message.substring(0, JunitReport.MAX_MESSAGE_LENGTH) + "…";
    }
}
