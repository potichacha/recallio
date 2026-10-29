package com.recallio.content.infrastructure.markdown;

import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.CardType;
import com.recallio.content.domain.source.ContentParser;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Lit le document de référence ({@code content/source/java-entretien-reference.md}, format décrit en CLAUDE.md, section
 * 5.1) :
 *
 * <ul>
 *   <li>un chapitre par titre {@code # N. TITRE} ;
 *   <li>{@code ### Questions techniques} : {@code **Q : question ?** R : réponse.}, ou une question suivie d'un bloc de
 *       code puis d'une ligne {@code R : ...} ;
 *   <li>{@code ### Questions de situation} : {@code **Q : "énoncé"**} puis {@code R : "réponse"} sur plusieurs lignes ;
 *   <li>le glossaire : un tableau Markdown {@code | **Terme** | définition |}.
 * </ul>
 *
 * <p>Une réponse s'étend jusqu'à la question suivante ou au prochain titre. Les lignes situées dans un bloc de code ne
 * sont jamais interprétées comme des titres.
 */
@ApplicationScoped
public class MarkdownReferenceParser implements ContentParser {

    /** Le chapitre des questions pièges « qu'affiche ce code ? » produit des cartes {@code CODE_OUTPUT}. */
    static final int CODE_OUTPUT_CHAPTER = 14;

    static final String GLOSSARY_KEY = "glossary";
    private static final int GLOSSARY_POSITION = 1000;
    private static final int EXCERPT_LENGTH = 80;

    private static final Pattern CHAPTER = Pattern.compile("^# (\\d+)\\. (.+)$");
    private static final Pattern GLOSSARY = Pattern.compile("^# GLOSSAIRE\\b.*$");
    private static final Pattern ANY_TITLE = Pattern.compile("^#{1,3} .*$");
    private static final Pattern SEPARATOR = Pattern.compile("^═+\\s*$");
    private static final Pattern FENCE = Pattern.compile("^\\s*```.*$");
    private static final Pattern INLINE_QUESTION = Pattern.compile("^\\*\\*Q : (.+?)\\*\\* R : (.+)$");
    /**
     * {@code **Q :**} ou {@code **Q : énoncé**}, suivi éventuellement d'une annotation comme {@code *(très fréquent)*}.
     * Le groupe est gourmand : un passage en gras à l'intérieur de l'énoncé ne le tronque pas.
     */
    private static final Pattern QUESTION_START =
            Pattern.compile("^\\*\\*Q :(?: (.*))?\\*\\*(?:\\s+\\*\\(.*\\)\\*)?\\s*$");

    private static final Pattern ANSWER_START = Pattern.compile("^R : ?(.*)$");
    private static final Pattern GLOSSARY_ROW = Pattern.compile("^\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*$");
    private static final Pattern TABLE_DELIMITER = Pattern.compile("^\\|[\\s:|-]+\\|\\s*$");

    private static final String TECHNICAL_SECTION = "### Questions techniques";
    private static final String SITUATION_SECTION = "### Questions de situation";
    private static final String GLOSSARY_HEADER_TERM = "Terme";

    private enum Section {
        IGNORED,
        TECHNICAL,
        SITUATION,
        GLOSSARY
    }

    @Override
    public ParsedContent parse(String source) {
        return new Parsing().run(source);
    }

    /** État d'une lecture : une instance par appel, le parseur lui-même reste sans état. */
    private static final class Parsing {

        private final List<ParsedTheme> themes = new ArrayList<>();
        private final List<SourceIssue> issues = new ArrayList<>();
        private @Nullable ThemeBuilder theme;
        private Section section = Section.IGNORED;
        private @Nullable PendingQuestion pending;
        private boolean inFence;

        ParsedContent run(String source) {
            String[] lines = source.split("\\R", -1);
            for (int index = 0; index < lines.length; index++) {
                accept(lines[index], index + 1);
            }
            closeTheme();
            return new ParsedContent(themes, issues);
        }

        private void accept(String line, int lineNumber) {
            if (FENCE.matcher(line).matches()) {
                inFence = !inFence;
                appendToBody(line, lineNumber);
                return;
            }
            if (inFence) {
                appendToBody(line, lineNumber);
                return;
            }
            if (SEPARATOR.matcher(line).matches()) {
                return;
            }
            if (ANY_TITLE.matcher(line).matches()) {
                onTitle(line);
                return;
            }
            switch (section) {
                case TECHNICAL, SITUATION -> onQuestionLine(line, lineNumber);
                case GLOSSARY -> onGlossaryLine(line, lineNumber);
                case IGNORED -> {
                    // Vue d'ensemble, cours, récapitulatif : pas de carte.
                }
            }
        }

        private void onTitle(String line) {
            closePending();
            Matcher chapter = CHAPTER.matcher(line);
            if (chapter.matches()) {
                int number = Integer.parseInt(chapter.group(1));
                startTheme("%02d".formatted(number), chapter.group(2).strip(), number);
                section = Section.IGNORED;
            } else if (GLOSSARY.matcher(line).matches()) {
                startTheme(GLOSSARY_KEY, line.substring(2).strip(), GLOSSARY_POSITION);
                section = Section.GLOSSARY;
            } else if (line.startsWith("# ")) {
                closeTheme();
                section = Section.IGNORED;
            } else if (theme != null && line.equals(TECHNICAL_SECTION)) {
                section = Section.TECHNICAL;
            } else if (theme != null && line.equals(SITUATION_SECTION)) {
                section = Section.SITUATION;
            } else if (section != Section.GLOSSARY) {
                section = Section.IGNORED;
            }
        }

        private void onQuestionLine(String line, int lineNumber) {
            Matcher inline = INLINE_QUESTION.matcher(line);
            if (inline.matches()) {
                closePending();
                pending = new PendingQuestion(lineNumber, inline.group(1));
                answerStarted(inline.group(2));
                return;
            }
            Matcher start = QUESTION_START.matcher(line);
            if (start.matches()) {
                closePending();
                String prompt = start.group(1);
                pending = new PendingQuestion(lineNumber, prompt == null ? "" : prompt);
                return;
            }
            Matcher answer = ANSWER_START.matcher(line);
            if (answer.matches() && pending != null && !pending.hasAnswer()) {
                answerStarted(answer.group(1));
                return;
            }
            appendToBody(line, lineNumber);
        }

        /**
         * Une réponse technique tient sur une ligne : la carte est complète. Une réponse de situation peut s'étendre sur
         * plusieurs lignes (et contenir du code), jusqu'à la question suivante.
         */
        private void answerStarted(String firstAnswerLine) {
            if (pending == null) {
                return;
            }
            pending.startAnswer(firstAnswerLine);
            if (section == Section.TECHNICAL) {
                closePending();
            }
        }

        private void appendToBody(String line, int lineNumber) {
            if (pending != null) {
                pending.append(line);
            } else if ((section == Section.TECHNICAL || section == Section.SITUATION) && !line.isBlank()) {
                issues.add(new SourceIssue(lineNumber, "Ligne non reconnue hors d'une question", excerpt(line)));
            }
        }

        private void onGlossaryLine(String line, int lineNumber) {
            if (line.isBlank() || TABLE_DELIMITER.matcher(line).matches()) {
                return;
            }
            Matcher row = GLOSSARY_ROW.matcher(line);
            if (!row.matches()) {
                return;
            }
            String term = stripEmphasis(row.group(1));
            if (term.equals(GLOSSARY_HEADER_TERM) || theme == null) {
                return;
            }
            theme.cards.add(
                    new ParsedCard(new CardContent.Definition(term, row.group(2).strip()), lineNumber));
        }

        private void startTheme(String key, String title, int position) {
            closeTheme();
            theme = new ThemeBuilder(key, title, position);
        }

        private void closeTheme() {
            closePending();
            if (theme != null) {
                themes.add(theme.build());
                theme = null;
            }
        }

        private void closePending() {
            PendingQuestion question = pending;
            pending = null;
            if (question == null || theme == null) {
                return;
            }
            if (!question.hasAnswer()) {
                issues.add(new SourceIssue(
                        question.line, "Question sans réponse (ligne « R : » attendue)", excerpt(question.prompt())));
                return;
            }
            CardType type = section == Section.SITUATION
                    ? CardType.OPEN_SCENARIO
                    : theme.position == CODE_OUTPUT_CHAPTER ? CardType.CODE_OUTPUT : CardType.FLASHCARD;
            boolean quoted = type == CardType.OPEN_SCENARIO;
            String prompt = quoted ? unquote(question.prompt()) : question.prompt();
            String answer = quoted ? unquote(question.answer()) : question.answer();
            if (prompt.isBlank() || answer.isBlank()) {
                issues.add(new SourceIssue(question.line, "Question ou réponse vide", excerpt(question.prompt())));
                return;
            }
            theme.cards.add(new ParsedCard(CardContent.of(type, prompt, answer), question.line));
        }
    }

    /** Une question en cours de lecture : son énoncé (avec un éventuel bloc de code), puis sa réponse. */
    private static final class PendingQuestion {
        private final int line;
        private final List<String> promptLines = new ArrayList<>();
        private @Nullable List<String> answerLines;

        PendingQuestion(int line, String firstPromptLine) {
            this.line = line;
            promptLines.add(firstPromptLine);
        }

        boolean hasAnswer() {
            return answerLines != null;
        }

        void startAnswer(String firstAnswerLine) {
            List<String> lines = new ArrayList<>();
            lines.add(firstAnswerLine);
            answerLines = lines;
        }

        void append(String line) {
            (answerLines != null ? answerLines : promptLines).add(line);
        }

        String prompt() {
            return String.join("\n", promptLines).strip();
        }

        String answer() {
            return answerLines == null ? "" : String.join("\n", answerLines).strip();
        }
    }

    private static final class ThemeBuilder {
        private final String key;
        private final String title;
        private final int position;
        private final List<ParsedCard> cards = new ArrayList<>();

        ThemeBuilder(String key, String title, int position) {
            this.key = key;
            this.title = title;
            this.position = position;
        }

        ParsedTheme build() {
            return new ParsedTheme(key, slugOf(title), title, position, cards);
        }
    }

    static String slugOf(String title) {
        return CardIdentity.normalize(title).replace(' ', '-').toLowerCase(Locale.ROOT);
    }

    /** Retire les guillemets qui encadrent les énoncés et réponses des questions de situation. */
    private static String unquote(String text) {
        String stripped = text.strip();
        if (stripped.length() >= 2 && stripped.startsWith("\"") && stripped.endsWith("\"")) {
            return stripped.substring(1, stripped.length() - 1).strip();
        }
        return stripped;
    }

    private static String stripEmphasis(String text) {
        return text.replace("**", "").strip();
    }

    private static String excerpt(String text) {
        String stripped = text.strip();
        return stripped.length() <= EXCERPT_LENGTH ? stripped : stripped.substring(0, EXCERPT_LENGTH) + "…";
    }
}
