package com.recallio.gamification.domain.beta;

/**
 * Une carte que plusieurs signalements attendent encore (ADR 0076). La file des admins (ADR 0042) montre les
 * signalements un par un ; celle-ci montre où ils s'accumulent.
 *
 * @param openReports ses signalements qui ne sont pas encore traités
 */
public record ReportedCard(CardLabel card, long openReports) {}
