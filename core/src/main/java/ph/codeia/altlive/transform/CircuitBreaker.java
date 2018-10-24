package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class CircuitBreaker<T> implements Try<T> {

    public static class Tripped extends IllegalStateException {
        private Tripped(Throwable cause) {
            super("Failed too many times.", cause);
        }
    }

    public static class Partial<T> implements Function<Try<T>, CircuitBreaker<T>> {
        private final int failLimit;
        private final long halfOpenTimeout;
        private Function<Throwable, Boolean> predicate = t -> true;

        private Partial(int failLimit, long halfOpenTimeout) {
            this.failLimit = failLimit;
            this.halfOpenTimeout = halfOpenTimeout;
        }

        @Transformer
        public Partial<T> guard(Function<Throwable, Boolean> predicate) {
            this.predicate = predicate;
            return this;
        }

        @Override
        public CircuitBreaker<T> apply(Try<T> source) {
            return new CircuitBreaker<>(source, failLimit, halfOpenTimeout, predicate);
        }
    }

    @Transformer
    public static <T> Partial<T> of(int failLimit, long halfOpenTimeout) {
        return new Partial<>(failLimit, halfOpenTimeout);
    }

    @Transformer
    public static <T> Partial<T> of(int failLimit, long halfOpenTimeout, TimeUnit units) {
        return of(failLimit, units.toMillis(halfOpenTimeout));
    }

    private final Try<T> source;
    private final int failLimit;
    private final long halfOpenTimeout;
    private final Function<Throwable, Boolean> predicate;
    private int failCount;
    private long lastFailTime;
    private Throwable lastError;

    public CircuitBreaker(
            Try<T> source,
            int failLimit,
            long halfOpenTimeout,
            Function<Throwable, Boolean> predicate
    ) {
        this.source = source;
        this.failLimit = failLimit;
        this.halfOpenTimeout = halfOpenTimeout;
        this.predicate = predicate;
    }

    @Override
    public void select(Case<? super T> continuation) {
        if (failLimit < 1) {
            source.select(continuation);
            return;
        }
        boolean isCircuitOpen = failCount >= failLimit &&
                System.currentTimeMillis() - lastFailTime < halfOpenTimeout;
        if (isCircuitOpen) {
            continuation.error(new Tripped(lastError));
            return;
        }
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                synchronized (CircuitBreaker.this) {
                    failCount = 0;
                    lastFailTime = 0L;
                    lastError = null;
                }
                continuation.ok(t);
            }

            @Override
            public void error(@NonNull Throwable t) {
                try {
                    if (predicate.apply(t)) synchronized (CircuitBreaker.this) {
                        failCount += 1;
                        lastFailTime = System.currentTimeMillis();
                        lastError = t;
                    }
                    continuation.error(t);
                }
                catch (RuntimeException e) {
                    continuation.error(e);
                }
            }
        });
    }
}

