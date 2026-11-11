package com.recallio.user.application;

import com.recallio.platform.domain.PersonalData;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Le compte d'un utilisateur, au sens du RGPD (ADR 0056) : récupérer ses données, et les faire effacer. Chaque module
 * répond des siennes par son {@link PersonalData} ; ce service ne connaît aucune de leurs tables.
 */
@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);

    /**
     * @param sections par module, puis par table, les lignes de l'utilisateur
     */
    public record AccountExport(
            Instant exportedAt, Map<String, Map<String, List<Map<String, @Nullable Object>>>> sections) {}

    private final Instance<PersonalData> personalData;
    private final Clock clock;

    public AccountService(Instance<PersonalData> personalData, Clock clock) {
        this.personalData = personalData;
        this.clock = clock;
    }

    @Transactional
    public AccountExport export(UserRef user) {
        Map<String, Map<String, List<Map<String, @Nullable Object>>>> sections = new TreeMap<>();
        for (PersonalData module : personalData) {
            sections.put(module.section(), module.export(user.id()));
        }
        return new AccountExport(clock.instant(), sections);
    }

    /** Immédiat et définitif : tout s'efface dans une seule transaction, ou rien. */
    @Transactional
    public void delete(UserRef user) {
        for (PersonalData module : personalData) {
            module.erase(user.id());
        }
        // Sans identifiant ni nom : rien de personnel dans les journaux (CLAUDE.md, section 4.3).
        LOG.info("Un compte a été supprimé à la demande de son utilisateur");
    }
}
