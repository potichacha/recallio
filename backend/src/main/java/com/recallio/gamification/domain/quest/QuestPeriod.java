package com.recallio.gamification.domain.quest;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** La durée d'une quête (ADR 0040) : un jour, ou une semaine du lundi au dimanche, dans le fuseau de l'utilisateur. */
public enum QuestPeriod {
    DAY,
    WEEK;

    /** Le premier jour de la période qui contient ce jour : le jour lui-même, ou le lundi de sa semaine. */
    public LocalDate start(LocalDate day) {
        return this == DAY ? day : day.with(DayOfWeek.MONDAY);
    }

    /** Le jour où commence la période suivante. */
    public LocalDate end(LocalDate day) {
        return this == DAY ? day.plusDays(1) : start(day).plusWeeks(1);
    }
}
