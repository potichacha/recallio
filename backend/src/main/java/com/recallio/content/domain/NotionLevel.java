package com.recallio.content.domain;

/**
 * Le public visé par une notion (ADR 0025) : apprendre le sujet, préparer un entretien, entretenir un niveau d'expert.
 * Stocké tel quel en base : ne pas renommer une valeur existante.
 */
public enum NotionLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
