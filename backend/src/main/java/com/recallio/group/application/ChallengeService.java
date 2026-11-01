package com.recallio.group.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.domain.Challenge;
import com.recallio.group.domain.ChallengeRepository;
import com.recallio.group.domain.ChallengeRun;
import com.recallio.group.domain.GroupExceptions.ChallengeNotFoundException;
import com.recallio.group.domain.GroupExceptions.GroupNotFoundException;
import com.recallio.group.domain.GroupExceptions.NotEnoughCardsException;
import com.recallio.group.domain.GroupSettings;
import com.recallio.group.domain.GroupSettingsRepository;
import com.recallio.group.domain.StudyGroup;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.jspecify.annotations.Nullable;

/**
 * Les défis d'un groupe (CLAUDE.md, section 6.4 ; ADR 0020) : le créateur en lance un, les membres le voient avec son
 * classement. Jouer est l'affaire de {@link ChallengePlayService}.
 */
@ApplicationScoped
public class ChallengeService {

    /** La liste d'un groupe montre les défis récents ; les anciens n'ont plus d'intérêt. */
    static final int LISTED_CHALLENGES = 20;

    private final ChallengeRepository challenges;
    private final GroupAccess access;
    private final GroupSettingsRepository settings;
    private final CardDirectory cards;
    private final UserDirectory users;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public ChallengeService(
            ChallengeRepository challenges,
            GroupAccess access,
            GroupSettingsRepository settings,
            CardDirectory cards,
            UserDirectory users,
            IdGenerator idGenerator,
            Clock clock) {
        this.challenges = challenges;
        this.access = access;
        this.settings = settings;
        this.cards = cards;
        this.users = users;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Le créateur lance un défi : une série tirée au hasard parmi les QCM des sujets et thèmes du groupe.
     *
     * @throws NotEnoughCardsException si le groupe n'a pas assez de QCM pour la série demandée
     */
    @Transactional
    public ChallengeView create(UserRef user, UUID groupId, String title, int cardCount, int secondsPerCard) {
        access.ownedGroup(user, groupId);
        List<UUID> pool = new ArrayList<>(multipleChoiceCards(settings.findByGroup(groupId)));
        if (pool.size() < cardCount) {
            throw new NotEnoughCardsException(pool.size());
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<UUID> series = pool.subList(0, Math.max(cardCount, 0));
        Challenge challenge = Challenge.create(
                idGenerator.newId(), groupId, title, series, secondsPerCard, user.id(), clock.instant());
        challenges.create(challenge);
        return view(challenge, new ChallengeRun(challenge, List.of()));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ChallengeView> ofGroup(UserRef user, UUID groupId) {
        access.visibleGroup(user, groupId);
        return challenges.findByGroup(groupId, LISTED_CHALLENGES).stream()
                .map(challenge -> view(challenge, run(challenge, user)))
                .toList();
    }

    /** Le défi, la partie de l'utilisateur, et le classement de ceux qui l'ont terminé. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public ChallengeDetail detail(UserRef user, UUID challengeId) {
        Challenge challenge =
                challenges.findById(challengeId).orElseThrow(() -> new ChallengeNotFoundException(challengeId));
        StudyGroup group = visibleGroupOf(user, challenge);
        LeaderboardVisibility visibility = settings.findByGroup(group.id()).leaderboardVisibility();
        return new ChallengeDetail(
                view(challenge, run(challenge, user)),
                group.name().value(),
                visibility,
                standings(challenge, user, visibility));
    }

    /** Le groupe du défi, s'il est visible de l'utilisateur ; sinon le défi lui-même est introuvable. */
    StudyGroup visibleGroupOf(UserRef user, Challenge challenge) {
        try {
            return access.visibleGroup(user, challenge.groupId());
        } catch (GroupNotFoundException e) {
            throw new ChallengeNotFoundException(challenge.id());
        }
    }

    private List<UUID> multipleChoiceCards(GroupSettings group) {
        CardFilter filter = new CardFilter(
                Set.copyOf(group.subjectSlugs()),
                null,
                Set.copyOf(group.themeSlugs()),
                Set.of(CardType.MULTIPLE_CHOICE));
        return cards.activeCards(filter).stream().map(ActiveCard::cardId).toList();
    }

    private ChallengeRun run(Challenge challenge, UserRef user) {
        return new ChallengeRun(challenge, challenges.attempts(challenge.id(), user.id()));
    }

    private ChallengeView view(Challenge challenge, ChallengeRun run) {
        ChallengeStatus status = run.isFinished()
                ? ChallengeStatus.FINISHED
                : run.isStarted() ? ChallengeStatus.IN_PROGRESS : ChallengeStatus.NOT_STARTED;
        return new ChallengeView(
                challenge, challenge.isOpenAt(clock.instant()), status, run.isFinished() ? run.score() : null);
    }

    /**
     * Les parties terminées, du meilleur score au moins bon, puis de la plus rapide à la plus lente ; les ex æquo
     * partagent leur rang. Le classement suit la visibilité choisie pour le groupe (ADR 0018).
     */
    private List<Standing> standings(Challenge challenge, UserRef viewer, LeaderboardVisibility visibility) {
        List<FinishedRun> finished = challenges.attemptsByUser(challenge.id()).entrySet().stream()
                .filter(entry -> visibility != LeaderboardVisibility.HIDDEN
                        || entry.getKey().equals(viewer.id()))
                .map(entry -> new FinishedRun(entry.getKey(), new ChallengeRun(challenge, entry.getValue())))
                .filter(entry -> entry.run().isFinished())
                .sorted(Comparator.comparing(FinishedRun::run, BEST_FIRST))
                .toList();
        Map<UUID, String> names =
                users.displayNames(finished.stream().map(FinishedRun::userId).toList());
        List<Standing> standings = new ArrayList<>(finished.size());
        for (int index = 0; index < finished.size(); index++) {
            FinishedRun current = finished.get(index);
            boolean tied =
                    index > 0 && BEST_FIRST.compare(finished.get(index - 1).run(), current.run()) == 0;
            boolean me = current.userId().equals(viewer.id());
            standings.add(new Standing(
                    tied ? standings.get(index - 1).rank() : index + 1,
                    me || visibility == LeaderboardVisibility.NAMED ? names.get(current.userId()) : null,
                    me,
                    current.run().score(),
                    current.run().time()));
        }
        return List.copyOf(standings);
    }

    private static final Comparator<ChallengeRun> BEST_FIRST =
            Comparator.comparingInt(ChallengeRun::score).reversed().thenComparing(ChallengeRun::time);

    private record FinishedRun(UUID userId, ChallengeRun run) {}

    public enum ChallengeStatus {
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    /**
     * @param open vrai tant qu'on peut y jouer
     * @param myScore le score de l'utilisateur, une fois sa partie terminée
     */
    public record ChallengeView(
            Challenge challenge,
            boolean open,
            ChallengeStatus myStatus,
            @Nullable Integer myScore) {}

    public record ChallengeDetail(
            ChallengeView challenge, String groupName, LeaderboardVisibility visibility, List<Standing> standings) {}

    /** @param displayName absent dans un classement anonymisé, sauf pour soi-même */
    public record Standing(int rank, @Nullable String displayName, boolean me, int score, Duration time) {}
}
