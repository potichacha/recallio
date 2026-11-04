package com.recallio.review.domain.playground;

/**
 * Comment s'est terminé un programme du Tour (ADR 0037). Son propre nom, et non un {@code Status} imbriqué : la spec
 * OpenAPI nomme les énumérations par leur nom simple, et celle des exercices s'appelle déjà {@code Status}.
 */
public enum ProgramStatus {
    /** Le programme est allé au bout, quel que soit son code de sortie. */
    COMPLETED,
    /** Le code ne compile pas : le message contient les erreurs du compilateur. */
    COMPILATION_ERROR,
    /** Le programme a dépassé son délai ; ses sorties sont celles écrites jusque-là. */
    TIMEOUT
}
