package com.recallio.review.domain.track;

import com.recallio.content.CardType;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.stats.CardCounts;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Où en est un utilisateur sur une notion (ADR 0034) : son niveau de maîtrise, et ce qui mène au niveau suivant.
 *
 * <p>Les niveaux s'enchaînent, chacun supposant le précédent : <em>acquise</em> reprend la règle du parcours (ADR 0032)
 * et ne se perd pas ; <em>maîtrisée</em> et <em>experte</em> demandent des cartes dont on se souvient durablement, dans
 * plusieurs formats, et se perdent dès qu'une carte oubliée repasse en réapprentissage.
 *
 * @param cardCount les cartes actives de la notion
 * @param formats les formats de ces cartes
 * @param seen les cartes actives déjà vues
 * @param recalled les cartes de la notion réussies au moins une fois, comptées comme le parcours les compte
 * @param recalledFormats les formats réussis au moins une fois
 * @param mastered les cartes maîtrisées : stabilité d'au moins {@link CardCounts#MASTERED_STABILITY_DAYS} jours
 * @param lasting les cartes retenues sur la durée : stabilité d'au moins {@link #LASTING_STABILITY_DAYS} jours
 */
public record NotionMastery(
        int cardCount,
        Set<CardType> formats,
        int seen,
        int recalled,
        Set<CardType> recalledFormats,
        StableCards mastered,
        StableCards lasting) {

    /** Les cartes stables à réunir pour maîtriser la notion : autant que de cartes réussies pour l'acquérir. */
    public static final int CARDS_TO_MASTER = LearningPath.RECALLED_TO_ACQUIRE;

    /** Les formats différents à réunir parmi ces cartes. */
    public static final int FORMATS_TO_MASTER = 3;

    /** La stabilité d'une carte retenue sur la durée : environ trois mois. */
    public static final double LASTING_STABILITY_DAYS = 90;

    public NotionMastery {
        formats = Set.copyOf(formats);
        recalledFormats = Set.copyOf(recalledFormats);
    }

    /**
     * @param cards les cartes actives de la notion, avec leur format
     * @param states la mémoire de l'utilisateur ; une carte absente est nouvelle
     * @param recalled les cartes de la notion réussies au moins une fois, y compris celles retirées depuis : elles
     *     comptent pour l'acquérir, comme dans le parcours
     */
    public static NotionMastery of(Map<UUID, CardType> cards, Map<UUID, ReviewState> states, Set<UUID> recalled) {
        Set<CardType> formats = EnumSet.noneOf(CardType.class);
        Set<CardType> recalledFormats = EnumSet.noneOf(CardType.class);
        var mastered = new StableCards.Counter();
        var lasting = new StableCards.Counter();
        int seen = 0;
        for (var card : cards.entrySet()) {
            CardType format = card.getValue();
            formats.add(format);
            if (recalled.contains(card.getKey())) {
                recalledFormats.add(format);
            }
            ReviewState state = states.get(card.getKey());
            if (state != null) {
                seen++;
                mastered.add(state.isStable(CardCounts.MASTERED_STABILITY_DAYS), format);
                lasting.add(state.isStable(LASTING_STABILITY_DAYS), format);
            }
        }
        return new NotionMastery(
                cards.size(), formats, seen, recalled.size(), recalledFormats, mastered.result(), lasting.result());
    }

    public MasteryLevel level() {
        if (seen == 0) {
            return MasteryLevel.NEW;
        }
        if (!LearningPath.isAcquired(recalled, cardCount)) {
            return MasteryLevel.DISCOVERED;
        }
        if (!isEnough(mastered)) {
            return MasteryLevel.ACQUIRED;
        }
        if (!isEnough(lasting) || !missingFormats().isEmpty()) {
            return MasteryLevel.MASTERED;
        }
        return MasteryLevel.EXPERT;
    }

    /** Ce qui mène au niveau suivant ; rien au niveau le plus haut, ni pour une notion sans carte. */
    public Optional<NextLevel> nextLevel() {
        if (cardCount == 0) {
            return Optional.empty();
        }
        return switch (level()) {
            case NEW -> Optional.of(NextLevel.forCards(MasteryLevel.DISCOVERED, seen, 1));
            case DISCOVERED ->
                Optional.of(
                        NextLevel.forCards(MasteryLevel.ACQUIRED, recalled, LearningPath.cardsToAcquire(cardCount)));
            case ACQUIRED -> Optional.of(towards(MasteryLevel.MASTERED, mastered, Set.of()));
            case MASTERED -> Optional.of(towards(MasteryLevel.EXPERT, lasting, missingFormats()));
            case EXPERT -> Optional.empty();
        };
    }

    /** Assez de cartes stables, dans assez de formats : toutes les cartes, ou tous les formats, d'une petite notion. */
    private boolean isEnough(StableCards stable) {
        return stable.count() >= cardsNeeded() && stable.formats().size() >= formatsNeeded();
    }

    private NextLevel towards(MasteryLevel level, StableCards stable, Set<CardType> missingFormats) {
        return new NextLevel(
                level, stable.count(), cardsNeeded(), stable.formats().size(), formatsNeeded(), missingFormats);
    }

    private int cardsNeeded() {
        return Math.min(CARDS_TO_MASTER, cardCount);
    }

    private int formatsNeeded() {
        return Math.min(FORMATS_TO_MASTER, formats.size());
    }

    /** Les formats de la notion encore jamais réussis : le niveau « experte » les demande tous. */
    private Set<CardType> missingFormats() {
        Set<CardType> missing = EnumSet.noneOf(CardType.class);
        missing.addAll(formats);
        missing.removeAll(recalledFormats);
        return missing;
    }

    /**
     * Les cartes d'une notion qui atteignent une stabilité.
     *
     * @param formats leurs formats
     */
    public record StableCards(int count, Set<CardType> formats) {

        public StableCards {
            formats = Set.copyOf(formats);
        }

        /** Compte les cartes stables au fil du parcours des cartes de la notion. */
        private static final class Counter {

            private final Set<CardType> formats = EnumSet.noneOf(CardType.class);
            private int count;

            void add(boolean stable, CardType format) {
                if (stable) {
                    count++;
                    formats.add(format);
                }
            }

            StableCards result() {
                return new StableCards(count, formats);
            }
        }
    }

    /**
     * Ce qui mène au niveau suivant, pour que la PWA le dise sans connaître les seuils.
     *
     * @param level le niveau visé
     * @param cards les cartes qui comptent déjà pour ce niveau : vues, réussies, maîtrisées ou retenues sur la durée
     * @param cardsNeeded les cartes qu'il demande
     * @param formats les formats différents parmi elles ; 0 si le niveau n'en demande pas
     * @param formatsNeeded les formats différents qu'il demande ; 0 s'il n'en demande pas
     * @param missingFormats les formats de la notion encore jamais réussis, que le niveau « experte » demande
     */
    public record NextLevel(
            MasteryLevel level,
            int cards,
            int cardsNeeded,
            int formats,
            int formatsNeeded,
            Set<CardType> missingFormats) {

        public NextLevel {
            missingFormats = Set.copyOf(missingFormats);
        }

        static NextLevel forCards(MasteryLevel level, int cards, int cardsNeeded) {
            return new NextLevel(level, cards, cardsNeeded, 0, 0, Set.of());
        }
    }
}
