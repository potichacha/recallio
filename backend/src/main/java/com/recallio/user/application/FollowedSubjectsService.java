package com.recallio.user.application;

import com.recallio.content.SubjectDirectory;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import com.recallio.user.domain.AuthenticatedUser;
import com.recallio.user.domain.FollowedSubjectRepository;
import com.recallio.user.domain.InvalidProfileException;
import com.recallio.user.domain.UserProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : choisir les sujets qu'on suit (CLAUDE.md, sections 2.5 et 6.1). Ils alimentent la file de révision ;
 * arrêter d'en suivre un ne supprime pas la progression.
 */
@ApplicationScoped
public class FollowedSubjectsService implements FollowedSubjects {

    public static final String UNKNOWN_SUBJECT = "user.unknown-subject";

    private final FollowedSubjectRepository repository;
    private final SubjectDirectory subjects;
    private final UserProfileService profiles;
    private final Clock clock;

    public FollowedSubjectsService(
            FollowedSubjectRepository repository, SubjectDirectory subjects, UserProfileService profiles, Clock clock) {
        this.repository = repository;
        this.subjects = subjects;
        this.profiles = profiles;
        this.clock = clock;
    }

    /**
     * Les sujets suivis, dans l'ordre du catalogue. Un sujet suivi qui n'est plus publié n'y figure pas : il réapparaît
     * s'il est publié de nouveau.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<String> followed(UUID userId) {
        Set<String> followed = repository.findSlugs(userId);
        return subjects.publishedSubjectSlugs().stream()
                .filter(followed::contains)
                .toList();
    }

    @Override
    public List<String> of(UserRef user) {
        return followed(user.id());
    }

    /**
     * Remplace les sujets suivis par l'utilisateur connecté.
     *
     * @throws InvalidProfileException si un slug ne désigne aucun sujet publié
     */
    @Transactional
    public List<String> follow(AuthenticatedUser user, List<String> slugs) {
        List<String> published = subjects.publishedSubjectSlugs();
        Set<String> requested = new LinkedHashSet<>(slugs);
        Optional<String> unknown =
                requested.stream().filter(slug -> !published.contains(slug)).findFirst();
        if (unknown.isPresent()) {
            throw new InvalidProfileException(
                    UNKNOWN_SUBJECT, "Aucun sujet publié ne correspond à « " + unknown.get() + " ».");
        }
        UserProfile profile = profiles.currentProfile(user, Optional.empty());
        repository.replace(profile.id(), requested, clock.instant());
        return followed(profile.id());
    }
}
