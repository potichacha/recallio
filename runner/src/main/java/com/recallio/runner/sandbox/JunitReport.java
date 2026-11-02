package com.recallio.runner.sandbox;

import com.recallio.runner.sandbox.RunResult.TestOutcome;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Lit le rapport XML de JUnit (format « legacy », {@code TEST-junit-jupiter.xml}) produit dans le bac à sable.
 *
 * <p>Le rapport vient d'un conteneur qui exécute du code non fiable : le parseur refuse toute déclaration de type de
 * document, donc toute entité externe (XXE), et les messages sont tronqués.
 */
final class JunitReport {

    static final int MAX_MESSAGE_LENGTH = 2_000;

    private static final String TEST_CASE = "testcase";
    /** Les éléments qui marquent un test raté : une assertion fausse, ou une exception inattendue. */
    private static final List<String> FAILURES = List.of("failure", "error");

    private JunitReport() {}

    /** @throws IllegalArgumentException si le rapport n'est pas un XML lisible */
    static List<TestOutcome> parse(String xml) {
        try {
            NodeList cases =
                    builder().parse(new InputSource(new StringReader(xml))).getElementsByTagName(TEST_CASE);
            List<TestOutcome> outcomes = new ArrayList<>();
            for (int index = 0; index < cases.getLength(); index++) {
                outcomes.add(outcome((Element) cases.item(index)));
            }
            return outcomes;
        } catch (SAXException | IOException e) {
            throw new IllegalArgumentException("Rapport JUnit illisible : " + e.getMessage(), e);
        }
    }

    private static TestOutcome outcome(Element testCase) {
        String name = testCase.getAttribute("name").replaceFirst("\\(\\)$", "");
        for (String failure : FAILURES) {
            NodeList found = testCase.getElementsByTagName(failure);
            if (found.getLength() > 0) {
                Element element = (Element) found.item(0);
                String message = element.getAttribute("message");
                return new TestOutcome(
                        testCase.getAttribute("classname"),
                        name,
                        false,
                        truncate(message.isEmpty() ? element.getAttribute("type") : message));
            }
        }
        return new TestOutcome(testCase.getAttribute("classname"), name, true, "");
    }

    private static String truncate(String text) {
        return text.length() <= MAX_MESSAGE_LENGTH ? text : text.substring(0, MAX_MESSAGE_LENGTH) + "…";
    }

    private static DocumentBuilder builder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Le parseur XML du JDK doit accepter ces options", e);
        }
    }
}
