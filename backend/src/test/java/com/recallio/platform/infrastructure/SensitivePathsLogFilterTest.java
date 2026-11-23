package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.logging.json.runtime.JsonFormatter;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

/** Les logs du serveur ne contiennent ni jeton d'invitation, ni adresse de profil, ni chaîne de requête (ADR 0070). */
class SensitivePathsLogFilterTest {

    /** Ce qu'écrit le gestionnaire d'erreurs de Quarkus pour une erreur 500, en style printf. */
    private static final String QUARKUS_ERROR_HANDLER = "HTTP Request to %s failed, error id: %s";

    private final SensitivePathsLogFilter filter = new SensitivePathsLogFilter();

    @Test
    void shouldMaskTheLineOfTheQuarkusErrorHandlerInTheJsonLog() {
        ExtLogRecord record =
                printf(Level.ERROR, QUARKUS_ERROR_HANDLER, "/api/v1/invitations/SECRETTOKEN/accept?code=abc", "4f1c-2");

        assertThat(filter.isLoggable(record)).isTrue();
        String line = new JsonFormatter().format(record);

        assertThat(line)
                .contains("\"message\":\"HTTP Request to /api/v1/invitations/:token/accept failed, error id: 4f1c-2\"")
                .doesNotContain("SECRETTOKEN")
                .doesNotContain("code=abc");
    }

    @Test
    void shouldMaskTheHandleOfAPublicProfileInALineInTheMessageFormatStyle() {
        var record = new ExtLogRecord(
                Level.WARN,
                "Profil {0} illisible",
                ExtLogRecord.FormatStyle.MESSAGE_FORMAT,
                getClass().getName());
        record.setParameters(new Object[] {"/api/v1/public-profiles/alice-dupont"});

        assertThat(filter.isLoggable(record)).isTrue();

        assertThat(new JsonFormatter().format(record))
                .contains("\"message\":\"Profil /api/v1/public-profiles/:handle illisible\"")
                .doesNotContain("alice");
    }

    @Test
    void shouldMaskALineThatIsNotFromJbossLogManager() {
        var record = new LogRecord(java.util.logging.Level.WARNING, "Échec de {0}");
        record.setParameters(new Object[] {"/api/v1/public-profiles/alice-dupont?tab=secret"});

        assertThat(filter.isLoggable(record)).isTrue();

        assertThat(record.getMessage()).isEqualTo("Échec de /api/v1/public-profiles/:handle");
        assertThat(record.getParameters()).isNull();
    }

    @Test
    void shouldKeepALineWithNothingToMaskUntouched() {
        ExtLogRecord record = printf(Level.INFO, "Import du sujet %s : %d ajoutées", "go", 3);

        assertThat(filter.isLoggable(record)).isTrue();

        assertThat(record.getMessage()).isEqualTo("Import du sujet %s : %d ajoutées");
        assertThat(record.getParameters()).containsExactly("go", 3);
        assertThat(record.getFormatStyle()).isEqualTo(ExtLogRecord.FormatStyle.PRINTF);
    }

    @Test
    void shouldKeepTheExceptionOfAMaskedLineAndItsStackTrace() {
        var failure = new IllegalStateException("Base injoignable");
        ExtLogRecord record = printf(
                Level.ERROR, QUARKUS_ERROR_HANDLER, "/api/v1/public-profiles/alice-dupont?q=secretquery", "4f1c-2");
        record.setThrown(failure);

        assertThat(filter.isLoggable(record)).isTrue();
        String line = new JsonFormatter().format(record);

        assertThat(record.getThrown()).isSameAs(failure);
        assertThat(line)
                .contains("HTTP Request to /api/v1/public-profiles/:handle failed")
                .contains("IllegalStateException")
                .contains("Base injoignable")
                .contains("shouldKeepTheExceptionOfAMaskedLineAndItsStackTrace")
                .doesNotContain("alice")
                .doesNotContain("secretquery");
    }

    private static ExtLogRecord printf(java.util.logging.Level level, String format, Object... parameters) {
        var record = new ExtLogRecord(
                level, format, ExtLogRecord.FormatStyle.PRINTF, SensitivePathsLogFilterTest.class.getName());
        record.setParameters(parameters);
        return record;
    }
}
