package com.recallio.user.application;

import com.recallio.content.SubjectDirectory;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : choisir les sujets qu'on suit (CLAUDE.md, sections 2.5 et 6.1). Ils alimentent la file de révision ;
 * arrêter d'en suivre un ne supprime pas la progression.
 */
@ApplicationScoped
public class FollowedSubjectsService implements FollowedSubjects {

    public static final String UNKNOWN_SUBJECT = "user.unknown-subject";
    public static final String NOT_FOLLOWED = "user.subject-not-followed";

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

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<String, LearningTrack> tracksOf(UserRef user) {
        return tracks(user.id());
    }

    /** Le parcours de chaque sujet suivi et publié, par slug. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<String, LearningTrack> tracks(UUID userId) {
        Map<String, LearningTrack> tracks = repository.findTracks(userId);
        return followed(userId).stream()
                .collect(Collectors.toMap(slug -> slug, slug -> tracks.getOrDefault(slug, LearningTrack.DEFAULT)));
    }

    /**
     * Choisit le parcours d'un sujet suivi (ADR 0032).
     *
     * @throws InvalidProfileException si l'utilisateur ne suit pas ce sujet
     */
    @Transactional
    public void chooseTrack(AuthenticatedUser user, String slug, LearningTrack track) {
        UserProfile profile = profiles.currentProfile(user, Optional.empty());
        if (!repository.updateTrack(profile.id(), slug, track)) {
            throw new InvalidProfileException(
                    NOT_FOLLOWED, "Le sujet « " + slug + " » n'est pas suivi : il faut d'abord le suivre.");
        }
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
