package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Delay<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Delay<T>> {
        private final long before;
        private final long after;

        private Partial(long before, long after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public Delay<T> apply(Try<T> source) {
            return new Delay<>(source, before, after);
        }
    }

    @Transformer
    public static <T> Partial<T> by(long before, long after) {
        return new Partial<>(before, after);
    }

    @Transformer
    public static <T> Partial<T> by(long before, long after, TimeUnit units) {
        return by(units.toMillis(before), units.toMillis(after));
    }

    @Transformer
    public static <T> Partial<T> executionBy(long before) {
        return by(before, 0L);
    }

    @Transformer
    public static <T> Partial<T> executionBy(long before, TimeUnit units) {
        return executionBy(units.toMillis(before));
    }

    @Transformer
    public static <T> Partial<T> resultBy(long after) {
        return by(0L, after);
    }

    @Transformer
    public static <T> Partial<T> resultBy(long after, TimeUnit units) {
        return resultBy(units.toMillis(after));
    }

    static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    private final Try<T> source;
    private final long before;
    private final long after;

    public Delay(Try<T> source, long before, long after) {
        this.source = source;
        this.before = before;
        this.after = after;
    }

    @Override
    public void select(Case<? super T> continuation) {
        Case<T> cont = new Case<T>() {
            @Override
            public void ok(T t) {
                if (after <= 0L) {
                    continuation.ok(t);
                }
                else {
                    SCHEDULER.schedule(
                            () -> continuation.ok(t),
                            after,
                            TimeUnit.MILLISECONDS
                    );
                }
            }

            @Override
            public void error(@NonNull Throwable t) {
                if (after <= 0L) {
                    continuation.error(t);
                }
                else {
                    SCHEDULER.schedule(
                            () -> continuation.error(t),
                            after,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        };
        if (before <= 0L) {
            source.select(cont);
        }
        else {
            SCHEDULER.schedule(
                    () -> source.select(cont),
                    before,
                    TimeUnit.MILLISECONDS
            );
        }
    }
}

