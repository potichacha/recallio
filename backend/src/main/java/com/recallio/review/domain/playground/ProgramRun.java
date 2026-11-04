package com.recallio.review.domain.playground;

/**
 * Ce qu'a donné un programme du Tour (ADR 0037) : l'exemple d'une page, ou ce qu'en a fait l'utilisateur.
 *
 * @param exitCode le code de sortie du programme ; 0 s'il n'a pas été lancé
 * @param truncated si l'une des deux sorties a été tronquée par le runner
 * @param message les erreurs de compilation, ou ce qui a arrêté le programme ; vide sinon
 */
public record ProgramRun(
        ProgramStatus status, int exitCode, String stdout, String stderr, boolean truncated, String message) {}
