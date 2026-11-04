package com.recallio.runner.sandbox;

/**
 * Ce qu'a donné un programme (ADR 0037).
 *
 * @param exitCode le code de sortie du programme ; 0 s'il n'a pas été lancé
 * @param stdout ce qu'il a écrit sur sa sortie standard, tronqué à {@value #MAX_OUTPUT_BYTES} octets
 * @param stderr ce qu'il a écrit sur sa sortie d'erreur, tronqué de même
 * @param truncated si l'une des deux sorties a été tronquée
 * @param message les erreurs de compilation, ou ce qui a arrêté le programme ou le runner ; vide sinon
 */
public record ProgramResult(
        Status status, int exitCode, String stdout, String stderr, boolean truncated, String message) {

    /** Ce que l'image garde de chaque sortie (le {@code MAX_OUTPUT} de son {@code run.sh}). */
    public static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public enum Status {
        /** Le programme est allé au bout, quel que soit son code de sortie. */
        COMPLETED,
        /** Le code ne compile pas : {@code message} contient les erreurs du compilateur. */
        COMPILATION_ERROR,
        /** Le programme a dépassé son délai et a été arrêté ; ses sorties sont celles écrites jusque-là. */
        TIMEOUT,
        /** Le runner n'a pas pu lancer le programme : une panne, jamais la faute de l'utilisateur. */
        ERROR
    }

    static ProgramResult failure(Status status, String message) {
        return new ProgramResult(status, 0, "", "", false, message);
    }
}
