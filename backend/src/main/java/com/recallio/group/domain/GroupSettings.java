package com.recallio.group.domain;

import com.recallio.content.CardType;
import java.util.List;
import java.util.Set;

/**
 * Les réglages d'un groupe (CLAUDE.md, section 6.4 ; ADR 0017) : ce que ses membres révisent ensemble, et comment le
 * classement les compare.
 *
 * @param subjectSlugs les sujets du groupe, dans l'ordre choisi ; aucun tant que le créateur ne les a pas choisis
 * @param themeSlugs restreint un sujet à certains de ses thèmes ; un sujet dont aucun thème n'est listé les garde tous
 * @param cardTypes les formats de carte retenus ; aucun : tous, y compris ceux qui apparaîtront plus tard
 * @param dailyGoal cartes à réviser par jour pour qu'une journée compte dans le classement du groupe
 */
public record GroupSettings(
        List<String> subjectSlugs,
        List<String> themeSlugs,
        Set<CardType> cardTypes,
        int dailyGoal,
        LeaderboardVisibility leaderboardVisibility) {

    public static final int MAX_SUBJECTS = 50;
    public static final int MAX_THEMES = 500;
    public static final int MIN_DAILY_GOAL = 1;
    public static final int MAX_DAILY_GOAL = 200;

    /** Le même objectif que la révision personnelle par défaut : un groupe neuf ne demande pas plus. */
    public static final int DEFAULT_DAILY_GOAL = 10;

    /** Les réglages d'un groupe neuf. */
    public static final GroupSettings DEFAULTS =
            new GroupSettings(List.of(), List.of(), Set.of(), DEFAULT_DAILY_GOAL, LeaderboardVisibility.NAMED);

    public GroupSettings {
        subjectSlugs = subjectSlugs.stream().distinct().toList();
        themeSlugs = themeSlugs.stream().distinct().toList();
        cardTypes = Set.copyOf(cardTypes);
        if (subjectSlugs.size() > MAX_SUBJECTS || themeSlugs.size() > MAX_THEMES) {
            throw new InvalidGroupException("group.too-many-subjects", "Trop de sujets ou de thèmes pour un groupe.");
        }
        if (dailyGoal < MIN_DAILY_GOAL || dailyGoal > MAX_DAILY_GOAL) {
            throw new InvalidGroupException(
                    "group.invalid-daily-goal",
                    "L'objectif quotidien doit être compris entre " + MIN_DAILY_GOAL + " et " + MAX_DAILY_GOAL
                            + " cartes.");
        }
    }
}
