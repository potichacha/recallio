package com.recallio.entitlement.application;

import com.recallio.entitlement.PlusAccess;
import com.recallio.entitlement.domain.AccessCode;
import com.recallio.entitlement.domain.AccessCodeRepository;
import com.recallio.entitlement.domain.CodeAttemptRepository;
import com.recallio.entitlement.domain.EntitlementExceptions.AccessCodeAlreadyRedeemedException;
import com.recallio.entitlement.domain.EntitlementExceptions.InvalidAccessCodeException;
import com.recallio.entitlement.domain.EntitlementExceptions.TooManyCodeAttemptsException;
import com.recallio.entitlement.domain.PlusEntitlement;
import com.recallio.entitlement.domain.PlusEntitlementRepository;
import com.recallio.entitlement.domain.RedeemableCode;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : le droit « Plus » d'un compte, et le code d'accès qui l'ouvre ou le prolonge (ADR 0079). Pas de
 * paiement : pendant la bêta, seuls des codes donnent Plus.
 *
 * <p>Un code qui ne marche pas donne toujours la même erreur, et compte comme un essai raté du compte. Ni le code ni
 * son empreinte ne sont écrits dans un log.
 */
@ApplicationScoped
public class PlusService implements PlusAccess {

    private static final Logger LOG = Logger.getLogger(PlusService.class);

    private final PlusEntitlementRepository entitlements;
    private final AccessCodeRepository codes;
    private final CodeAttemptRepository attempts;
    private final Clock clock;

    public PlusService(
            PlusEntitlementRepository entitlements,
            AccessCodeRepository codes,
            CodeAttemptRepository attempts,
            Clock clock) {
        this.entitlements = entitlements;
        this.codes = codes;
        this.attempts = attempts;
        this.clock = clock;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean hasPlus(UUID userId) {
        Instant now = clock.instant();
        return entitlements
                .find(userId)
                .filter(entitlement -> entitlement.isActiveAt(now))
                .isPresent();
    }

    /** Le droit du compte, maintenant. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public PlusStatus status(UserRef user) {
        return PlusStatus.of(entitlements.find(user.id()).orElse(null), clock.instant());
    }

    /**
     * Ce que donnerait ce code, sans l'utiliser : l'écran l'annonce avant de demander de l'activer. Un essai raté compte
     * comme à l'utilisation, sinon cette route servirait à deviner des codes.
     *
     * @throws TooManyCodeAttemptsException si le compte a épuisé ses essais de l'heure
     * @throws AccessCodeAlreadyRedeemedException s'il a déjà utilisé ce code
     * @throws InvalidAccessCodeException si le code est inconnu, mal formé, épuisé, expiré ou révoqué
     */
    @Transactional(dontRollbackOn = InvalidAccessCodeException.class)
    public CodeOffer check(UserRef user, String input) {
        Instant now = clock.instant();
        refuseIfBlocked(user, now);
        Optional<RedeemableCode> found = AccessCode.parse(input).flatMap(code -> codes.find(code.fingerprint()));
        return new CodeOffer(redeemable(found, user, now).batch().plusDays());
    }

    /**
     * Utilise un code : le droit du compte finit {@code plusDays} jours après sa fin actuelle, ou après maintenant s'il
     * n'en a pas ou plus.
     *
     * @throws TooManyCodeAttemptsException si le compte a épuisé ses essais de l'heure
     * @throws AccessCodeAlreadyRedeemedException s'il a déjà utilisé ce code
     * @throws InvalidAccessCodeException si le code est inconnu, mal formé, épuisé, expiré ou révoqué
     */
    // Un essai raté s'enregistre dans la transaction que l'exception termine : elle ne doit pas l'annuler.
    @Transactional(dontRollbackOn = InvalidAccessCodeException.class)
    public Redemption redeem(UserRef user, String input) {
        Instant now = clock.instant();
        refuseIfBlocked(user, now);
        // Le code est verrouillé : un autre compte qui l'utilise au même moment attend, puis le trouve compté.
        Optional<RedeemableCode> found = AccessCode.parse(input).flatMap(code -> codes.lock(code.fingerprint()));
        RedeemableCode code = redeemable(found, user, now);
        codes.redeem(code, user.id(), now);
        int plusDays = code.batch().plusDays();
        PlusEntitlement extended = entitlements.lock(user.id(), now).extendedBy(plusDays, now);
        entitlements.save(user.id(), extended, now);
        LOG.infof(
                "Un code du lot %s a été utilisé : %d jours de Plus",
                code.batch().id(), plusDays);
        return new Redemption(plusDays, PlusStatus.of(extended, now));
    }

    private void refuseIfBlocked(UserRef user, Instant now) {
        if (attempts.isBlocked(user.id(), now)) {
            throw new TooManyCodeAttemptsException();
        }
    }

    /** Le code, s'il marche pour ce compte ; sinon l'essai raté est compté. */
    private RedeemableCode redeemable(Optional<RedeemableCode> found, UserRef user, Instant now) {
        // Avant tout autre contrôle : un code à usage unique que ce compte a utilisé est aussi un code épuisé.
        if (found.isPresent() && codes.hasRedeemed(found.get().id(), user.id())) {
            throw new AccessCodeAlreadyRedeemedException();
        }
        return found.filter(code -> code.isRedeemableAt(now)).orElseThrow(() -> {
            attempts.recordFailure(user.id(), now);
            return new InvalidAccessCodeException();
        });
    }

    /**
     * @param endsAt la fin du droit, passée s'il est échu ; {@code null} si le compte n'a jamais eu Plus
     * @param daysLeft les jours qu'il reste, un jour entamé comptant pour un ; 0 sans Plus
     */
    public record PlusStatus(boolean active, @Nullable Instant endsAt, int daysLeft) {

        static PlusStatus of(@Nullable PlusEntitlement entitlement, Instant now) {
            return entitlement == null
                    ? new PlusStatus(false, null, 0)
                    : new PlusStatus(entitlement.isActiveAt(now), entitlement.endsAt(), entitlement.daysLeftAt(now));
        }
    }

    /** @param plusDays les jours de Plus que le code donnerait */
    public record CodeOffer(int plusDays) {}

    /**
     * @param plusDays les jours de Plus que le code a donnés
     * @param plus le droit du compte, après lui
     */
    public record Redemption(int plusDays, PlusStatus plus) {}
}
