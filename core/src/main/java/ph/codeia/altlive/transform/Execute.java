package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Execute<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Execute<T>> {
        private final Executor executor;

        private Partial(Executor executor) {
            this.executor = executor;
        }

        @Override
        public Execute<T> apply(Try<T> source) {
            return new Execute<>(source, executor);
        }
    }

    @Transformer
    public static <T> Partial<T> on(Executor executor) {
        return new Partial<>(executor);
    }

    public static <T> Execute<T> on(Executor executor, Callable<? extends T> block) {
        return new Execute<>(Try.of(block), executor);
    }

    private final Try<T> source;
    private final Executor executor;

    public Execute(Try<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }

    @Override
    public void select(Case<? super T> continuation) {
        executor.execute(() -> source.select(continuation));
    }
}
