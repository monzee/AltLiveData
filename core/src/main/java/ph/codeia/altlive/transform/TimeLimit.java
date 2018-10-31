package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class TimeLimit<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, TimeLimit<T>> {
        private final long timeoutMillis;

        private Partial(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public TimeLimit<T> apply(Try<T> source) {
            return new TimeLimit<>(source, timeoutMillis);
        }
    }

    @Transformer
    public static <T> Partial<T> of(long timeout, TimeUnit units) {
        return of(units.toMillis(timeout));
    }

    @Transformer
    public static <T> Partial<T> of(long timeoutMillis) {
        return new Partial<>(timeoutMillis);
    }

    private final Try<T> source;
    private final long timeoutMillis;

    public TimeLimit(Try<T> source, long timeoutMillis) {
        this.source = source;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void select(Case<? super T> continuation) {
        AtomicBoolean done = new AtomicBoolean(false);
        if (timeoutMillis > 0) {
            Delay.SCHEDULER.schedule(() -> {
                if (!done.getAndSet(true)) {
                    continuation.error(new TimeoutException());
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS);
        }
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                if (!done.getAndSet(true)) {
                    continuation.ok(t);
                }
            }

            @Override
            public void error(@NonNull Throwable t) {
                if (!done.getAndSet(true)) {
                    continuation.error(t);
                }
            }
        });
    }
}

