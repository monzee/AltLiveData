package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Retry<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Retry<T>> {
        private final int maxRetries;
        private Function<Throwable, Boolean> predicate = t -> true;
        private Function<Integer, Long> delay = i -> 0L;

        private Partial(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Transformer
        public Partial<T> guard(Function<Throwable, Boolean> predicate) {
            this.predicate = predicate;
            return this;
        }

        @Transformer
        public Partial<T> backoff(Function<Integer, Long> delay) {
            this.delay = delay;
            return this;
        }

        @Override
        public Retry<T> apply(Try<T> source) {
            return new Retry<>(source, maxRetries, predicate, delay);
        }
    }

    @Transformer
    public static <T> Partial<T> until(int maxRetries) {
        return new Partial<>(maxRetries);
    }

    @Transformer
    public static <T> Partial<T> forever() {
        return new Partial<>(0);
    }

    private final Try<T> source;
    private final int maxRetries;
    private final Function<Throwable, Boolean> predicate;
    private final Function<Integer, Long> delay;

    public Retry(
            Try<T> source,
            int maxRetries,
            Function<Throwable, Boolean> predicate,
            Function<Integer, Long> delay
    ) {
        this.source = source;
        this.maxRetries = maxRetries;
        this.predicate = predicate;
        this.delay = delay;
    }

    @Override
    public void select(Case<? super T> continuation) {
        source.select(new Case<T>() {
            int retries = 0;

            @Override
            public void ok(T t) {
                continuation.ok(t);
            }

            @Override
            public void error(@NonNull Throwable t) {
                try {
                    boolean canRetry = maxRetries < 1 || retries < maxRetries;
                    boolean shouldRetry = canRetry && predicate.apply(t);
                    if (!shouldRetry) {
                        continuation.error(t);
                    }
                    else {
                        long millis = delay.apply(retries);
                        retries += 1;
                        if (millis <= 0) {
                            source.select(this);
                        }
                        else {
                            Delay.SCHEDULER.schedule(
                                    () -> source.select(this),
                                    millis,
                                    TimeUnit.MILLISECONDS
                            );
                        }
                    }
                }
                catch (RuntimeException e) {
                    continuation.error(e);
                }
            }
        });
    }
}

