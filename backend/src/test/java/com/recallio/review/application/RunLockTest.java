package com.recallio.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RunLockTest {

    private final RunLock lock = new RunLock();

    @Test
    void shouldRefuseASecondRunOfTheSameUserWhileTheFirstIsInProgress() throws Exception {
        UUID user = UUID.randomUUID();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> lock.alone(
                user,
                () -> {
                    started.countDown();
                    await(release);
                    return "premier";
                },
                Busy::new));
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> lock.alone(user, () -> "second", Busy::new)).isInstanceOf(Busy.class);
        assertThat(lock.alone(UUID.randomUUID(), () -> "un autre utilisateur", Busy::new))
                .isEqualTo("un autre utilisateur");

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("premier");
        assertThat(lock.alone(user, () -> "après le premier", Busy::new)).isEqualTo("après le premier");
    }

    @Test
    void shouldReleaseTheLockWhenTheRunFails() {
        UUID user = UUID.randomUUID();

        assertThatThrownBy(() -> lock.alone(
                        user,
                        () -> {
                            throw new IllegalStateException("panne");
                        },
                        Busy::new))
                .isInstanceOf(IllegalStateException.class);

        assertThat(lock.alone(user, () -> "ensuite", Busy::new)).isEqualTo("ensuite");
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static final class Busy extends RuntimeException {}
}
