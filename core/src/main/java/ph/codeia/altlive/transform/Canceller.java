package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Canceller<T> implements Try<T> {

    public static class Partial<T> implements Function<Try<T>, Canceller<T>> {
        private final Runnable onCancel;

        private Partial(Runnable onCancel) {
            this.onCancel = onCancel;
        }

        @Override
        public Canceller<T> apply(Try<T> source) {
            return new Canceller<>(source, onCancel);
        }
    }

    @Transformer
    public static <T> Partial<T> of(Runnable onCancel) {
        return new Partial<>(onCancel);
    }

    private final Try<T> source;
    private final Runnable onCancel;
    private volatile boolean isCancelled = false;

    public Canceller(Try<T> source, Runnable onCancel) {
        this.source = source;
        this.onCancel = onCancel;
    }

    @Transformer
    public Canceller(Try<T> source) {
        this(source, () -> {});
    }

    public synchronized void cancel() {
        if (!isCancelled) {
            isCancelled = true;
            onCancel.run();
        }
    }

    @Override
    public void select(Case<? super T> continuation) {
        if (isCancelled) {
            return;
        }
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                if (!isCancelled) {
                    continuation.ok(t);
                }
            }

            @Override
            public void error(@NonNull Throwable t) {
                if (!isCancelled) {
                    continuation.error(t);
                }
            }
        });
    }
}

