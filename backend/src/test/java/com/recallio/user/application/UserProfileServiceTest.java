package com.recallio.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.platform.RecordingEvent;
import com.recallio.user.AccountCreated;
import com.recallio.user.UserRef;
import com.recallio.user.domain.AuthenticatedUser;
import com.recallio.user.domain.DisplayName;
import com.recallio.user.domain.InvalidProfileException;
import com.recallio.user.domain.UserProfile;
import com.recallio.user.domain.UserProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final AuthenticatedUser ALICE = new AuthenticatedUser("sub-alice", "Alice Martin");

    private final InMemoryProfiles repository = new InMemoryProfiles();
    private final RecordingEvent<AccountCreated> accountCreated = new RecordingEvent<>();
    private final UserProfileService service =
            new UserProfileService(repository, UUID::randomUUID, Clock.fixed(NOW, ZoneOffset.UTC), accountCreated);

    @Test
    void shouldAnnounceTheAccountWhenTheProfileIsCreatedAndNeverAgain() {
        UserProfile profile = service.currentProfile(ALICE, Optional.of("Europe/Paris"));
        service.currentProfile(ALICE, Optional.empty());

        assertThat(accountCreated.fired())
                .containsExactly(new AccountCreated(new UserRef(profile.id(), ZoneId.of("Europe/Paris")), NOW));
    }

    @Test
    void shouldNotAnnounceTheAccountCreatedMeanwhileByAnotherLogin() {
        // Une première connexion simultanée, dans un deuxième onglet, a créé le profil entre la lecture et l'insertion.
        repository.createdMeanwhile =
                new UserProfile(UUID.randomUUID(), "sub-alice", new DisplayName("Alice"), ZoneId.of("UTC"), NOW);

        service.currentProfile(ALICE, Optional.empty());

        assertThat(accountCreated.fired()).isEmpty();
    }

    @Test
    void shouldCreateTheProfileOnFirstLoginWithTheBrowserTimezone() {
        UserProfile profile = service.currentProfile(ALICE, Optional.of("Europe/Paris"));

        assertThat(profile.oidcSubject()).isEqualTo("sub-alice");
        assertThat(profile.displayName().value()).isEqualTo("Alice Martin");
        assertThat(profile.timezone()).isEqualTo(ZoneId.of("Europe/Paris"));
        assertThat(profile.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReturnTheSameProfileOnLaterLoginsAndIgnoreTheBrowserTimezone() {
        UserProfile first = service.currentProfile(ALICE, Optional.of("Europe/Paris"));

        UserProfile again = service.currentProfile(ALICE, Optional.of("America/New_York"));

        assertThat(again).isEqualTo(first);
    }

    @Test
    void shouldFallBackToUtcWhenTheBrowserTimezoneIsMissingOrInvalid() {
        assertThat(service.currentProfile(ALICE, Optional.of("Mars/Olympus")).timezone())
                .isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldUpdateDisplayNameAndTimezone() {
        service.currentProfile(ALICE, Optional.empty());

        UserProfile updated = service.updatePreferences(ALICE, " Alice M. ", "Asia/Tokyo");

        assertThat(updated.displayName().value()).isEqualTo("Alice M.");
        assertThat(updated.timezone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        assertThat(repository.findBySubject("sub-alice")).contains(updated);
    }

    @Test
    void shouldRejectAnUnknownTimezone() {
        assertThatThrownBy(() -> service.updatePreferences(ALICE, "Alice", "Mars/Olympus"))
                .isInstanceOf(InvalidProfileException.class)
                .hasMessageContaining("Mars/Olympus");
    }

    /** Faux dépôt en mémoire : teste le cas d'usage sans base de données. */
    private static final class InMemoryProfiles implements UserProfileRepository {
        private final Map<String, UserProfile> bySubject = new HashMap<>();

        /** Un profil qu'une autre requête insère juste avant celui du test. */
        @Nullable
        UserProfile createdMeanwhile;

        @Override
        public Optional<UserProfile> findBySubject(String oidcSubject) {
            return Optional.ofNullable(bySubject.get(oidcSubject));
        }

        @Override
        public Optional<UserProfile> findById(UUID id) {
            return bySubject.values().stream()
                    .filter(profile -> profile.id().equals(id))
                    .findFirst();
        }

        @Override
        public UserProfile createIfAbsent(UserProfile candidate) {
            UserProfile other = createdMeanwhile;
            if (other != null) {
                bySubject.putIfAbsent(other.oidcSubject(), other);
            }
            return bySubject.computeIfAbsent(candidate.oidcSubject(), subject -> candidate);
        }

        @Override
        public void update(UserProfile profile) {
            bySubject.put(profile.oidcSubject(), profile);
        }

        @Override
        public Map<UUID, DisplayName> findDisplayNames(Collection<UUID> ids) {
            return bySubject.values().stream()
                    .filter(profile -> ids.contains(profile.id()))
                    .collect(Collectors.toMap(UserProfile::id, UserProfile::displayName));
        }
    }
}
