package com.recallio.live.application;

/** Ce que l'on mesure du mode live (ADR 0024) ; implémenté avec Micrometer dans l'infrastructure. */
public interface LiveMetrics {

    void gameStarted();

    void gameFinished();

    void answerReceived();
}
