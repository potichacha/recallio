package com.recallio.review.application;

import com.recallio.review.domain.fsrs.FsrsParameters;
import com.recallio.review.domain.fsrs.FsrsScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/** Le planificateur est du Java pur : il est construit ici, avec les poids FSRS-6 par défaut (ADR 0011). */
public class SchedulerProducer {

    @Produces
    @ApplicationScoped
    FsrsScheduler scheduler(ReviewConfig config) {
        return new FsrsScheduler(FsrsParameters.DEFAULT.withDesiredRetention(config.desiredRetention()));
    }
}
