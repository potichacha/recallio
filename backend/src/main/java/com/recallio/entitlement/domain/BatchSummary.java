package com.recallio.entitlement.domain;

/**
 * Un lot, avec ce que ses codes sont devenus.
 *
 * @param codes le nombre de codes créés
 * @param usedCodes ceux qu'au moins un compte a utilisés
 * @param redemptions le nombre total d'utilisations
 */
public record BatchSummary(AccessCodeBatch batch, int codes, int usedCodes, int redemptions) {

    /**
     * Un code du lot, tel qu'un admin le reconnaît : ses derniers caractères, jamais le code.
     *
     * @param redemptions le nombre de comptes qui l'ont utilisé
     */
    public record Code(String suffix, int redemptions) {}
}
