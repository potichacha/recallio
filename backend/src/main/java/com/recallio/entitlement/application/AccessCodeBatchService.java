package com.recallio.entitlement.application;

import com.recallio.entitlement.domain.AccessCode;
import com.recallio.entitlement.domain.AccessCodeBatch;
import com.recallio.entitlement.domain.AccessCodeRepository;
import com.recallio.entitlement.domain.AccessCodeRepository.NewCode;
import com.recallio.entitlement.domain.BatchSummary;
import com.recallio.entitlement.domain.EntitlementExceptions.BatchAlreadyExpiredException;
import com.recallio.entitlement.domain.EntitlementExceptions.BatchNotFoundException;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage d'un admin : créer un lot de codes d'accès, suivre ses lots, en révoquer un (ADR 0079). Les codes ne sont
 * rendus qu'à la création : ensuite, la base n'en connaît que l'empreinte.
 */
@ApplicationScoped
public class AccessCodeBatchService {

    private static final Logger LOG = Logger.getLogger(AccessCodeBatchService.class);

    private final AccessCodeRepository codes;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AccessCodeBatchService(AccessCodeRepository codes, IdGenerator idGenerator, Clock clock) {
        this.codes = codes;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * @param codeCount le nombre de codes à tirer, de 1 à {@value AccessCodeBatch#MAX_CODES}
     * @param expiresAt quand les codes cessent de marcher ; {@code null} s'ils n'expirent pas
     * @throws BatchAlreadyExpiredException si cette date est déjà passée
     */
    @Transactional
    public CreatedBatch create(
            String label, int codeCount, int plusDays, int maxRedemptionsPerCode, @Nullable Instant expiresAt) {
        if (codeCount < 1 || codeCount > AccessCodeBatch.MAX_CODES) {
            throw new IllegalArgumentException("Un lot compte de 1 à %d codes".formatted(AccessCodeBatch.MAX_CODES));
        }
        Instant now = clock.instant();
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new BatchAlreadyExpiredException();
        }
        var batch = new AccessCodeBatch(
                idGenerator.newId(), label.strip(), plusDays, maxRedemptionsPerCode, expiresAt, now, null);
        List<String> readable = new ArrayList<>(codeCount);
        List<NewCode> stored = new ArrayList<>(codeCount);
        for (int index = 0; index < codeCount; index++) {
            AccessCode code = AccessCode.generate(random);
            readable.add(code.formatted());
            stored.add(new NewCode(idGenerator.newId(), code.fingerprint(), code.suffix()));
        }
        codes.add(batch, stored);
        LOG.infof("Lot de codes %s créé : %d codes, %d jours de Plus chacun", batch.id(), codeCount, plusDays);
        return new CreatedBatch(new BatchSummary(batch, codeCount, 0, 0), readable);
    }

    /** Les lots, le plus récent d'abord. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<BatchSummary> list(int page, int size) {
        return codes.list(page, size);
    }

    /**
     * Un lot et ses codes, reconnus à leurs derniers caractères.
     *
     * @throws BatchNotFoundException si le lot n'existe pas
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public BatchDetail detail(UUID batchId) {
        return new BatchDetail(summary(batchId), codes.codesOf(batchId));
    }

    /**
     * Révoque un lot : ses codes ne marchent plus, les droits qu'ils ont déjà ouverts restent. Révoquer un lot révoqué
     * ne change rien.
     *
     * @throws BatchNotFoundException si le lot n'existe pas
     */
    @Transactional
    public BatchSummary revoke(UUID batchId) {
        if (codes.revoke(batchId, clock.instant())) {
            LOG.infof("Lot de codes %s révoqué", batchId);
        }
        return summary(batchId);
    }

    private BatchSummary summary(UUID batchId) {
        return codes.find(batchId).orElseThrow(() -> new BatchNotFoundException(batchId));
    }

    /** @param codes les codes, lisibles cette seule fois, dans l'ordre de leur création */
    public record CreatedBatch(BatchSummary summary, List<String> codes) {

        public CreatedBatch {
            codes = List.copyOf(codes);
        }
    }

    public record BatchDetail(BatchSummary summary, List<BatchSummary.Code> codes) {

        public BatchDetail {
            codes = List.copyOf(codes);
        }
    }
}
