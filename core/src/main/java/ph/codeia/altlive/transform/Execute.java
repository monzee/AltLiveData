package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.Executor;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Execute<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Execute<T>> {
        private final Executor executor;

        private Partial(Executor executor) {
            this.executor = executor;
        }

        public <U> Function<Try<U>, Execute<U>> within(long timeoutMillis) {
            return within(Timed.millis(timeoutMillis));
        }

        public <U> Function<Try<U>, Execute<U>> within(Timed.Partial<U> timeout) {
            return source -> timeout
                    .on(executor)
                    .apply(source)
                    .pipe(new Partial<>(executor));
        }

        @Override
        public Execute<T> apply(Try<T> source) {
            return new Execute<>(executor, source);
        }
    }

    public static <T> Partial<T> on(Executor executor) {
        return new Partial<>(executor);
    }

    private final Executor executor;
    private final Try<T> source;

    public Execute(Executor executor, Try<T> source) {
        this.executor = executor;
        this.source = source;
    }

    @Override
    public void select(Case<? super T> continuation) {
        executor.execute(() -> source.select(continuation));
    }
}
