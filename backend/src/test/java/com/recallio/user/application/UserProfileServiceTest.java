package com.recallio.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.user.domain.AuthenticatedUser;
import com.recallio.user.domain.InvalidProfileException;
import com.recallio.user.domain.UserProfile;
import com.recallio.user.domain.UserProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final AuthenticatedUser ALICE = new AuthenticatedUser("sub-alice", "Alice Martin");

    private final InMemoryProfiles repository = new InMemoryProfiles();
    private final UserProfileService service =
            new UserProfileService(repository, UUID::randomUUID, Clock.fixed(NOW, ZoneOffset.UTC));

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

        @Override
        public Optional<UserProfile> findBySubject(String oidcSubject) {
            return Optional.ofNullable(bySubject.get(oidcSubject));
        }

        @Override
        public UserProfile createIfAbsent(UserProfile candidate) {
            return bySubject.computeIfAbsent(candidate.oidcSubject(), subject -> candidate);
        }

        @Override
        public void update(UserProfile profile) {
            bySubject.put(profile.oidcSubject(), profile);
        }
    }
}
