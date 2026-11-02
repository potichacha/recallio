package com.recallio.live.application;

import com.recallio.group.GroupMembers;
import com.recallio.group.GroupReviewScopes;
import com.recallio.live.domain.LiveExceptions.LiveGameAlreadyOpenException;
import com.recallio.live.domain.LiveExceptions.LiveGameNotFoundException;
import com.recallio.live.domain.LiveExceptions.SubjectNotInGroupException;
import com.recallio.live.domain.LiveGame;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.live.domain.LiveGameRepository;
import com.recallio.live.domain.LiveQuestion;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Les parties live d'un groupe (CLAUDE.md, section 6.5 ; ADR 0021 et 0022) : tout membre en lance une sur un sujet du
 * groupe, les membres la rejoignent, l'hôte la démarre. Chaque changement est diffusé aux joueurs ({@link
 * LiveGameEvents}), et chaque étape programme son propre réveil ({@link LiveScheduler}). Une partie d'un groupe dont
 * l'appelant n'est pas membre répond 404.
 *
 * <p>Pas de transaction ici : les parties vivent en mémoire, et les modules lus (group, user, content) ouvrent les leurs.
 * Le réveil d'une partie s'exécute sur la boucle d'événements, sans rien qui bloque.
 */
@ApplicationScoped
public class LiveGameService {

    /** Un membre dont le profil a disparu reste affiché, sans nom. */
    private static final String UNKNOWN_PLAYER = "Joueur";

    private final LiveGameRepository games;
    private final LiveQuestionDraw draw;
    private final LiveGameEvents events;
    private final LiveScheduler scheduler;
    private final GroupMembers members;
    private final GroupReviewScopes groupScopes;
    private final UserDirectory users;
    private final IdGenerator idGenerator;
    private final LiveConfig config;
    private final Clock clock;

    public LiveGameService(
            LiveGameRepository games,
            LiveQuestionDraw draw,
            LiveGameEvents events,
            LiveScheduler scheduler,
            GroupMembers members,
            GroupReviewScopes groupScopes,
            UserDirectory users,
            IdGenerator idGenerator,
            LiveConfig config,
            Clock clock) {
        this.games = games;
        this.draw = draw;
        this.events = events;
        this.scheduler = scheduler;
        this.members = members;
        this.groupScopes = groupScopes;
        this.users = users;
        this.idGenerator = idGenerator;
        this.config = config;
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
        closeIfOver(groupId, now);
        LiveGame game = new LiveGame(idGenerator.newId(), groupId, subjectSlug, user.id(), now);
        if (games.registerUnlessGroupHasOne(game).isPresent()) {
            throw new LiveGameAlreadyOpenException();
        }
        return game.snapshot();
    }

    /** La partie ouverte du groupe, s'il y en a une. */
    public Optional<Snapshot> openGameOf(UserRef user, UUID groupId) {
        members.requireMember(user, groupId);
        closeIfOver(groupId, clock.instant());
        return games.findOpenByGroup(groupId).map(LiveGame::snapshot);
    }

    public Snapshot game(UserRef user, UUID gameId) {
        return visibleGame(user, gameId).snapshot();
    }

    /** Une connexion de l'utilisateur à la partie ; il la rejoint où elle en est. */
    public Snapshot join(UserRef user, UUID gameId) {
        LiveGame game = visibleGame(user, gameId);
        String name = users.displayNames(List.of(user.id())).getOrDefault(user.id(), UNKNOWN_PLAYER);
        Instant before = game.phaseEndsAt();
        return changed(game, before, game.join(user.id(), name, clock.instant()));
    }

    /** Une connexion de moins. Une partie que son dernier joueur quitte est fermée. */
    public void leave(UUID userId, UUID gameId) {
        games.findById(gameId).ifPresent(game -> {
            Instant before = game.phaseEndsAt();
            Snapshot snapshot = game.leave(userId, clock.instant());
            if (game.isEmpty()) {
                games.remove(game);
            } else {
                changed(game, before, snapshot);
            }
        });
    }

    /** L'hôte démarre la partie : les questions sont tirées, la première est posée à tous. */
    public Snapshot start(UserRef user, UUID gameId) {
        LiveGame game = visibleGame(user, gameId);
        Set<String> themes = groupScopes.of(user, game.groupId()).themeSlugs();
        List<LiveQuestion> questions = draw.draw(game.subjectSlug(), themes);
        Instant before = game.phaseEndsAt();
        return changed(game, before, game.start(user.id(), questions, config.timing(), clock.instant()));
    }

    /** La réponse d'un joueur à la question en cours ; la correction n'est diffusée qu'à la fin de la question. */
    public Snapshot answer(UserRef user, UUID gameId, int questionIndex, Set<Integer> selected) {
        LiveGame game = visibleGame(user, gameId);
        Instant before = game.phaseEndsAt();
        return changed(game, before, game.answer(user.id(), questionIndex, selected, clock.instant()));
    }

    /**
     * Le réveil d'une partie, prévu pour l'échéance {@code dueAt} ; sur la boucle d'événements, il ne doit pas bloquer.
     * Un réveil périmé (l'étape a déjà changé : correction anticipée) est ignoré ; un réveil un peu en avance sur
     * l'horloge se reprogramme, sinon la partie resterait bloquée.
     */
    void wakeUp(UUID gameId, Instant dueAt) {
        games.findById(gameId).filter(game -> dueAt.equals(game.phaseEndsAt())).ifPresent(game -> {
            if (game.advance(clock.instant())) {
                changed(game, dueAt, game.snapshot());
            } else {
                scheduler.wakeUpAt(dueAt, () -> wakeUp(gameId, dueAt));
            }
        });
    }

    /**
     * Diffuse le nouvel état et, si l'échéance a changé (nouvelle étape), programme le réveil correspondant : une réponse
     * ou une arrivée ne multiplie pas les minuteurs.
     */
    private Snapshot changed(LiveGame game, @Nullable Instant endsBefore, Snapshot snapshot) {
        events.changed(snapshot);
        Instant endsAt = game.phaseEndsAt();
        if (endsAt != null && !endsAt.equals(endsBefore)) {
            scheduler.wakeUpAt(endsAt, () -> wakeUp(game.id(), endsAt));
        }
        return snapshot;
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

    private void closeIfOver(UUID groupId, Instant now) {
        games.findOpenByGroup(groupId).filter(game -> game.isOverAt(now)).ifPresent(games::remove);
    }
}
