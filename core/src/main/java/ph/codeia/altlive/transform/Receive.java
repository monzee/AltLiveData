package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Receive<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Receive<T>> {
        private final Executor executor;

        private Partial(Executor executor) {
            this.executor = executor;
        }

        @Override
        public Receive<T> apply(Try<T> source) {
            return new Receive<>(source, executor);
        }
    }

    @Transformer
    public static <T> Partial<T> on(Executor executor) {
        return new Partial<>(executor);
    }

    private final Try<T> source;
    private final Executor executor;

    public Receive(Try<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }

    @Override
    public void select(Case<? super T> continuation) {
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                executor.execute(() -> continuation.ok(t));
            }

            @Override
            public void error(@NonNull Throwable t) {
                executor.execute(() -> continuation.error(t));
            }
        });
    }
}

