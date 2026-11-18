package com.recallio.user.application;

import com.recallio.platform.domain.IdGenerator;
import com.recallio.user.AccountCreated;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import com.recallio.user.domain.AuthenticatedUser;
import com.recallio.user.domain.DisplayName;
import com.recallio.user.domain.Timezones;
import com.recallio.user.domain.UserProfile;
import com.recallio.user.domain.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cas d'usage du profil : création à la première connexion, consultation et préférences. */
@ApplicationScoped
public class UserProfileService implements UserDirectory {

    private final UserProfileRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final Event<AccountCreated> accountCreated;

    public UserProfileService(
            UserProfileRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            Event<AccountCreated> accountCreated) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.accountCreated = accountCreated;
    }

    /**
     * Le profil de l'utilisateur connecté, créé s'il se connecte pour la première fois.
     *
     * @param browserTimezone fuseau détecté par le navigateur, utilisé seulement à la création
     */
    @Transactional
    public UserProfile currentProfile(AuthenticatedUser user, Optional<String> browserTimezone) {
        return repository.findBySubject(user.subject()).orElseGet(() -> create(user, browserTimezone));
    }

    @Transactional
    public UserProfile updatePreferences(AuthenticatedUser user, String displayName, String timezone) {
        UserProfile current = currentProfile(user, Optional.empty());
        UserProfile updated = current.withPreferences(new DisplayName(displayName), Timezones.require(timezone));
        repository.update(updated);
        return updated;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<UserRef> user(UUID userId) {
        return repository.findById(userId).map(profile -> new UserRef(profile.id(), profile.timezone()));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<UUID, String> displayNames(Collection<UUID> userIds) {
        return repository.findDisplayNames(userIds).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().value()));
    }

    private UserProfile create(AuthenticatedUser user, Optional<String> browserTimezone) {
        ZoneId timezone = browserTimezone.flatMap(Timezones::parse).orElse(Timezones.DEFAULT);
        var candidate = new UserProfile(
                idGenerator.newId(),
                user.subject(),
                DisplayName.suggestedFrom(user.suggestedName()),
                timezone,
                clock.instant());
        UserProfile profile = repository.createIfAbsent(candidate);
        // Deux premières connexions simultanées ne créent qu'un profil : seule celle dont le candidat a été retenu
        // annonce le compte (ADR 0064).
        if (profile.id().equals(candidate.id())) {
            accountCreated.fire(new AccountCreated(new UserRef(profile.id(), profile.timezone()), profile.createdAt()));
        }
        return profile;
    }
}
