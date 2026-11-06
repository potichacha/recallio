package com.recallio.gamification.application;

import com.recallio.content.CardType;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.XpLedger.XpEntry;
import com.recallio.gamification.domain.XpReason;
import com.recallio.gamification.domain.quest.Quest;
import com.recallio.gamification.domain.quest.QuestBoard;
import com.recallio.gamification.domain.quest.QuestEvent;
import com.recallio.gamification.domain.quest.QuestPeriod;
import com.recallio.gamification.domain.quest.QuestRepository;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.CardReviewed;
import com.recallio.review.TourNotionValidated;
import com.recallio.review.TourPageOpened;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fait avancer les quêtes du jour et de la semaine (ADR 0040), au fil des révisions et du Tour. Observateur synchrone,
 * exécuté dans la transaction de l'événement, comme l'XP des révisions : une quête terminée rapporte son XP une fois.
 */
@ApplicationScoped
public class QuestTracker {

    /** Les formats où l'on lit ou écrit du code. */
    static final Set<CardType> CODE_FORMATS =
            EnumSet.of(CardType.CODE_OUTPUT, CardType.BUG_FIX, CardType.FILL_IN, CardType.CODING_EXERCISE);

    private final QuestRepository quests;
    private final XpLedger ledger;
    private final IdGenerator idGenerator;
    private final Event<QuestCompleted> completed;

    public QuestTracker(
            QuestRepository quests, XpLedger ledger, IdGenerator idGenerator, Event<QuestCompleted> completed) {
        this.quests = quests;
        this.ledger = ledger;
        this.idGenerator = idGenerator;
        this.completed = completed;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onCardReviewed(@Observes CardReviewed event) {
        UUID userId = event.user().id();
        LocalDate day = LocalDate.ofInstant(event.reviewedAt(), event.user().timezone());
        LocalDate week = QuestPeriod.WEEK.start(day);
        List<QuestEvent> happened = new ArrayList<>();
        happened.add(QuestEvent.REVIEW);
        if (event.recalled() && event.gradedByServer()) {
            happened.add(QuestEvent.VERIFIED_SUCCESS);
        }
        if (CODE_FORMATS.contains(event.cardType())) {
            happened.add(QuestEvent.CODE_CARD);
        }
        if (event.recalled() && event.cardType() == CardType.CODING_EXERCISE) {
            happened.add(QuestEvent.EXERCISE_PASSED);
        }
        if (quests.firstTime(userId, week, "format:" + event.cardType())) {
            happened.add(QuestEvent.NEW_FORMAT);
        }
        if (quests.firstTime(userId, week, "day:" + day)) {
            happened.add(QuestEvent.ACTIVE_DAY);
        }
        happened.forEach(kind -> advance(event.user(), day, kind, event.reviewedAt()));
    }

    /** Une page compte une fois par jour : revenir dessus ne fait pas avancer la quête. */
    @Transactional(Transactional.TxType.MANDATORY)
    void onTourPageOpened(@Observes TourPageOpened event) {
        LocalDate day = LocalDate.ofInstant(event.openedAt(), event.user().timezone());
        String page = "page:" + event.subjectSlug() + "/" + event.notionSlug() + "/" + event.page();
        if (quests.firstTime(event.user().id(), day, page)) {
            advance(event.user(), day, QuestEvent.TOUR_PAGE, event.openedAt());
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onTourNotionValidated(@Observes TourNotionValidated event) {
        LocalDate day = LocalDate.ofInstant(event.validatedAt(), event.user().timezone());
        advance(event.user(), day, QuestEvent.TOUR_NOTION, event.validatedAt());
    }

    /** Fait avancer les quêtes de ce jour et de sa semaine qui attendent cet événement. */
    private void advance(UserRef user, LocalDate day, QuestEvent kind, Instant at) {
        for (QuestPeriod period : QuestPeriod.values()) {
            LocalDate start = period.start(day);
            for (Quest quest : QuestBoard.of(user.id(), period, day)) {
                if (quest.event() != kind) {
                    continue;
                }
                int progress = quests.advance(user.id(), quest, start, 1);
                if (progress >= quest.target() && quests.complete(user.id(), quest, start, at)) {
                    ledger.append(new XpEntry(
                            idGenerator.newId(),
                            user.id(),
                            quest.xp(),
                            XpReason.QUEST,
                            null,
                            sourceId(user.id(), quest, start),
                            at));
                    completed.fire(new QuestCompleted(user, quest, at));
                }
            }
        }
    }

    /** La source de l'XP d'une quête : la quête, dans sa période, pour cet utilisateur. Le registre ne la compte qu'une fois. */
    static UUID sourceId(UUID userId, Quest quest, LocalDate periodStart) {
        return UUID.nameUUIDFromBytes(
                ("quest:" + userId + ":" + quest + ":" + periodStart).getBytes(StandardCharsets.UTF_8));
    }
}
