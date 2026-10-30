package com.recallio.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardIdentityTest {

    @Test
    void shouldIgnoreCaseAccentsPunctuationAndMarkdownWhenNormalizing() {
        assertThat(CardIdentity.normalize("Qu'est-ce que **la JVM** ?")).isEqualTo("qu est-ce que la jvm");
        assertThat(CardIdentity.normalize("Écrire `equals()` — vraiment ?")).isEqualTo("ecrire equals vraiment");
    }

    @Test
    void shouldKeepOperatorsSoThatSymbolOnlyQuestionsStayDistinct() {
        assertThat(CardIdentity.normalize("`>>` vs `>>>` ?")).isEqualTo(">> vs >>>");
        assertThat(CardIdentity.cardId("java", "01", "`>>` vs `>>>` ?"))
                .isNotEqualTo(CardIdentity.cardId("java", "01", "`&` vs `&&` ?"));
    }

    @Test
    void shouldKeepTheSameCardIdWhenOnlyTheFormattingOfTheQuestionChanges() {
        assertThat(CardIdentity.cardId("java", "01", "Qu'est-ce que la JVM ?"))
                .isEqualTo(CardIdentity.cardId("java", "01", "Qu’est-ce que **la jvm**  ?"));
    }

    @Test
    void shouldCreateADifferentCardWhenTheQuestionMeaningChanges() {
        assertThat(CardIdentity.cardId("java", "01", "Qu'est-ce que la JVM ?"))
                .isNotEqualTo(CardIdentity.cardId("java", "01", "Qu'est-ce que le JIT ?"));
    }

    @Test
    void shouldScopeCardIdsToTheirTheme() {
        assertThat(CardIdentity.cardId("java", "01", "Même question ?"))
                .isNotEqualTo(CardIdentity.cardId("java", "02", "Même question ?"));
    }

    @Test
    void shouldScopeIdentifiersToTheirSubject() {
        assertThat(CardIdentity.cardId("java", "01", "Même question ?"))
                .isNotEqualTo(CardIdentity.cardId("go", "01", "Même question ?"));
        assertThat(CardIdentity.themeId("java", "01")).isNotEqualTo(CardIdentity.themeId("go", "01"));
    }

    @Test
    void shouldPrefixThemeSlugsWithTheirSubjectOnlyOnce() {
        assertThat(CardIdentity.themeSlug("java", "JAVA : FONDAMENTAUX")).isEqualTo("java-fondamentaux");
        assertThat(CardIdentity.themeSlug("go", "GLOSSAIRE DES SIGLES ET TERMES"))
                .isEqualTo("go-glossaire-des-sigles-et-termes");
        assertThat(CardIdentity.themeSlug("java", "QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?"))
                .isEqualTo("java-questions-pieges-qu-affiche-ce-code");
    }

    @Test
    void shouldChangeTheContentHashOnAnyTextChange() {
        var original = new CardContent.Flashcard("Q ?", "Réponse.");
        var reformatted = new CardContent.Flashcard("Q ?", "**Réponse**.");
        assertThat(CardIdentity.contentHash(original)).isNotEqualTo(CardIdentity.contentHash(reformatted));
    }
}
