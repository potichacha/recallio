package com.recallio.live.application;

import com.recallio.group.GroupMembers;
import com.recallio.group.GroupReviewScopes;
import com.recallio.live.domain.LiveExceptions.LiveGameAlreadyOpenException;
import com.recallio.live.domain.LiveExceptions.LiveGameNotFoundException;
import com.recallio.live.domain.LiveExceptions.SubjectNotInGroupException;
import com.recallio.live.domain.LiveGame;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.live.domain.LiveGameRepository;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les parties live d'un groupe (CLAUDE.md, section 6.5 ; ADR 0021) : tout membre en lance une sur un sujet du groupe,
 * les membres la rejoignent. Une partie d'un groupe dont l'appelant n'est pas membre répond 404.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class LiveGameService {

    /** Un membre dont le profil a disparu reste affiché, sans nom. */
    private static final String UNKNOWN_PLAYER = "Joueur";

    private final LiveGameRepository games;
    private final GroupMembers members;
    private final GroupReviewScopes groupScopes;
    private final UserDirectory users;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public LiveGameService(
            LiveGameRepository games,
            GroupMembers members,
            GroupReviewScopes groupScopes,
            UserDirectory users,
            IdGenerator idGenerator,
            Clock clock) {
        this.games = games;
        this.members = members;
        this.groupScopes = groupScopes;
        this.users = users;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Ouvre une partie, dont l'utilisateur est l'hôte.
     *
     * @throws SubjectNotInGroupException si le sujet n'est pas l'un de ceux du groupe
     * @throws LiveGameAlreadyOpenException si le groupe a déjà une partie ouverte
     */
    public Snapshot open(UserRef user, UUID groupId, String subjectSlug) {
        if (!groupScopes.of(user, groupId).subjectSlugs().contains(subjectSlug)) {
            throw new SubjectNotInGroupException(subjectSlug);
        }
        Instant now = clock.instant();
        closeIfAbandoned(groupId, now);
        LiveGame game = new LiveGame(idGenerator.newId(), groupId, subjectSlug, user.id(), now);
        if (games.registerUnlessGroupHasOne(game).isPresent()) {
            throw new LiveGameAlreadyOpenException();
        }
        return game.snapshot();
    }

    /** La partie ouverte du groupe, s'il y en a une. */
    public Optional<Snapshot> openGameOf(UserRef user, UUID groupId) {
        members.requireMember(user, groupId);
        closeIfAbandoned(groupId, clock.instant());
        return games.findOpenByGroup(groupId).map(LiveGame::snapshot);
    }

    public Snapshot game(UserRef user, UUID gameId) {
        return visibleGame(user, gameId).snapshot();
    }

    /** Une connexion de l'utilisateur à la partie. */
    public Snapshot join(UserRef user, UUID gameId) {
        LiveGame game = visibleGame(user, gameId);
        String name = users.displayNames(List.of(user.id())).getOrDefault(user.id(), UNKNOWN_PLAYER);
        return game.join(user.id(), name, clock.instant());
    }

    /**
     * Une connexion de moins. Une salle que son dernier joueur quitte est fermée.
     *
     * @return l'état à diffuser aux joueurs restants ; vide si la partie est fermée
     */
    public Optional<Snapshot> leave(UUID userId, UUID gameId) {
        return games.findById(gameId).flatMap(game -> {
            Snapshot snapshot = game.leave(userId);
            if (game.isEmpty()) {
                games.remove(game);
                return Optional.empty();
            }
            return Optional.of(snapshot);
        });
    }

    private LiveGame visibleGame(UserRef user, UUID gameId) {
        LiveGame game = games.findById(gameId).orElseThrow(() -> new LiveGameNotFoundException(gameId));
        try {
            members.requireMember(user, game.groupId());
        } catch (ResourceNotFoundException e) {
            throw new LiveGameNotFoundException(gameId);
        }
        return game;
    }

    private void closeIfAbandoned(UUID groupId, Instant now) {
        games.findOpenByGroup(groupId).filter(game -> game.isAbandonedAt(now)).ifPresent(games::remove);
    }
}
