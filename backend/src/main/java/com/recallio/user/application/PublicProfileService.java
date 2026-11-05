package com.recallio.user.application;

import com.recallio.user.PublicProfiles;
import com.recallio.user.UserRef;
import com.recallio.user.domain.AuthenticatedUser;
import com.recallio.user.domain.HandleTakenException;
import com.recallio.user.domain.InvalidProfileException;
import com.recallio.user.domain.PublicHandle;
import com.recallio.user.domain.PublicProfileRepository;
import com.recallio.user.domain.PublicProfileSettings;
import com.recallio.user.domain.UserProfile;
import com.recallio.user.domain.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage : l'adresse du profil public et sa visibilité (ADR 0043). Le profil est privé par défaut ; l'utilisateur
 * le rend public, ou privé de nouveau, à tout moment.
 */
@ApplicationScoped
public class PublicProfileService implements PublicProfiles {

    private final PublicProfileRepository repository;
    private final UserProfileRepository users;
    private final UserProfileService profiles;
    private final Clock clock;

    public PublicProfileService(
            PublicProfileRepository repository, UserProfileRepository users, UserProfileService profiles, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.profiles = profiles;
        this.clock = clock;
    }

    /** Les réglages de l'utilisateur connecté ; vides tant qu'il n'a pas choisi d'adresse. */
    @Transactional
    public Optional<PublicProfileSettings> settings(AuthenticatedUser user) {
        return repository.findByUser(
                profiles.currentProfile(user, Optional.empty()).id());
    }

    /** @throws HandleTakenException si un autre utilisateur a déjà cette adresse */
    @Transactional
    public PublicProfileSettings update(AuthenticatedUser user, String handle, boolean visible) {
        UUID userId = profiles.currentProfile(user, Optional.empty()).id();
        var wanted = new PublicHandle(handle);
        if (repository
                .findByHandle(wanted)
                .filter(owner -> !owner.userId().equals(userId))
                .isPresent()) {
            throw new HandleTakenException(wanted);
        }
        var settings = new PublicProfileSettings(userId, wanted, visible);
        repository.save(settings, clock.instant());
        return settings;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<PublicUser> visible(String handle) {
        PublicHandle wanted;
        try {
            wanted = new PublicHandle(handle);
        } catch (InvalidProfileException invalid) {
            // Une adresse mal formée n'existe pas : même réponse qu'un profil inconnu ou privé.
            return Optional.empty();
        }
        return repository
                .findByHandle(wanted)
                .filter(PublicProfileSettings::visible)
                .flatMap(settings ->
                        users.findById(settings.userId()).map(profile -> publicUser(settings.handle(), profile)));
    }

    private static PublicUser publicUser(PublicHandle handle, UserProfile profile) {
        return new PublicUser(
                new UserRef(profile.id(), profile.timezone()),
                handle.value(),
                profile.displayName().value());
    }
}
