package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Recover<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Recover<T>> {
        private final Function<Throwable, Try<T>> handler;

        private Partial(Function<Throwable, Try<T>> handler) {
            this.handler = handler;
        }

        @Override
        public Recover<T> apply(Try<T> source) {
            return new Recover<>(source, handler);
        }
    }

    @Transformer
    public static <T> Partial<T> flatFrom(Function<Throwable, Try<T>> handler) {
        return new Partial<>(handler);
    }

    @Transformer
    public static <T> Partial<T> from(Function.Checked<Throwable, T> handler) {
        return flatFrom(t -> {
            try {
                return Try.just(handler.apply(t));
            }
            catch (Throwable e) {
                return Try.raise(e);
            }
        });
    }

    private final Try<T> source;
    private final Function<Throwable, Try<T>> handler;

    public Recover(Try<T> source, Function<Throwable, Try<T>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    public void select(Case<? super T> continuation) {
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                continuation.ok(t);
            }

            @Override
            public void error(@NonNull Throwable t) {
                try {
                    handler.apply(t).select(continuation);
                }
                catch (RuntimeException e) {
                    continuation.error(e);
                }
            }
        });
    }
}

