package com.recallio.platform.infrastructure;

import io.quarkus.logging.LoggingFilter;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Masque, dans chaque ligne de la console, ce qu'une adresse de l'API porte de secret ou de personnel
 * ({@link SensitivePaths}) : les logs du serveur n'en contiennent pas (CLAUDE.md, section 4.3 ; ADR 0070). Par exemple,
 * le gestionnaire d'erreurs de Quarkus écrit « HTTP Request to /api/v1/invitations/&lt;jeton&gt;?… failed » pour une
 * erreur 500 : la ligne sort avec {@code /api/v1/invitations/:token}.
 *
 * <p>Un filtre de logs sert d'habitude à trier les lignes ; celui-ci les laisse toutes passer, mais réécrit leur
 * message. C'est le seul point d'accroche avant la sortie : le gestionnaire d'erreurs de Quarkus, qui écrit l'adresse
 * complète, n'est pas à nous, et le formateur n'offre aucun moyen de réécrire un message. Une ligne sans rien à masquer
 * reste intacte, paramètres compris ; l'exception d'une ligne et sa pile ne sont jamais touchées.
 *
 * <p>Branché sur la console par {@code quarkus.log.console.filter=sensitive-paths}, dans tous les profils.
 */
@LoggingFilter(name = "sensitive-paths")
public final class SensitivePathsLogFilter implements Filter {

    /**
     * Le message d'une ligne, formaté comme le fait le formateur JSON : selon le style de la ligne (printf,
     * {@code MessageFormat} ou aucun). JBoss LogManager ne garde pas le message formaté en cache : le formateur
     * reformate la ligne après ce filtre.
     */
    private static final ExtFormatter MESSAGE = new ExtFormatter() {
        @Override
        public String format(ExtLogRecord record) {
            return formatMessage(record);
        }
    };

    @Override
    public boolean isLoggable(LogRecord record) {
        String formatted = MESSAGE.formatMessage(record);
        if (formatted == null) {
            return true;
        }
        String masked = SensitivePaths.mask(formatted);
        if (!masked.equals(formatted)) {
            // Le texte masqué est déjà formaté : plus de paramètres à y appliquer, ni ici ni dans un autre handler.
            if (record instanceof ExtLogRecord extRecord) {
                extRecord.setMessage(masked, ExtLogRecord.FormatStyle.NO_FORMAT);
            } else {
                record.setMessage(masked);
            }
            record.setParameters(null);
        }
        return true;
    }
}
