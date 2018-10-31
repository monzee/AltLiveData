package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Filter<T> implements Try<T> {

    public static class NotFound extends NoSuchElementException {
        private static final NotFound INSTANCE = new NotFound();

        private NotFound() {
        }
    }

    public static class Partial<T> implements Function<Try<T>, Filter<T>> {
        private final Function<T, Boolean> predicate;

        private Partial(Function<T, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Filter<T> apply(Try<T> source) {
            return new Filter<>(source, predicate);
        }
    }

    @Transformer
    public static <T> Partial<T> accept(Function<T, Boolean> predicate) {
        return new Partial<>(predicate);
    }

    @Transformer
    public static <T> Partial<T> reject(Function<T, Boolean> predicate) {
        return accept(t -> !predicate.apply(t));
    }

    private final Try<T> source;
    private final Function<T, Boolean> predicate;

    public Filter(Try<T> source, Function<T, Boolean> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public void select(Case<? super T> continuation) {
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                try {
                    if (predicate.apply(t)) {
                        continuation.ok(t);
                    }
                    else {
                        continuation.error(NotFound.INSTANCE);
                    }
                }
                catch (RuntimeException e) {
                    continuation.error(e);
                }
            }

            @Override
            public void error(@NonNull Throwable t) {
                continuation.error(t);
            }
        });
    }
}

