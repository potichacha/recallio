package com.recallio.gamification.application;

import com.recallio.gamification.domain.quest.Quest;
import com.recallio.user.UserRef;
import java.time.Instant;

/** Événement interne au module : une quête vient d'être terminée (ADR 0040). Les succès s'en servent. */
public record QuestCompleted(UserRef user, Quest quest, Instant completedAt) {}
