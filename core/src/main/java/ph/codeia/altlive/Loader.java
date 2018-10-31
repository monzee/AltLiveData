package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.Nullable;

/**
 * Represents a computation that might fail and has a pending state.
 *
 * <p> This is very much like {@link Try} except there is a third branch that
 * denotes that the result is not yet available and is currently being computed
 * in another thread. This really only makes sense when used in a reactive
 * system.
 *
 * @param <T> The type of the value being computed
 */
public interface Loader<T> {
    /**
     * "Pattern matches" the state of the computation.
     */
    void select(Progress<? super T> continuation);

    /**
     * Converts the current state into a {@link Try}.
     *
     * <p> The {@code failed} case triggers the {@link Try.Case#error(Throwable)}
     * case while {@code running} and {@code ok} both trigger the
     * {@link Try.Case#ok(Object)} case.
     */
    default Try<T> toTry() {
        return continuation -> select(new Progress<T>() {
            @Override
            public void running(@Nullable T currentValue) {
                continuation.ok(currentValue);
            }

            @Override
            public void done(T value) {
                continuation.ok(value);
            }

            @Override
            public void failed(Throwable error) {
                continuation.error(error);
            }
        });
    }

    static <T> Progress<T> whenDone(Receiver<T> receiver) {
        return new Progress<T>() {
            @Override
            public void running(@Nullable T currentValue) {
            }

            @Override
            public void done(T value) {
                receiver.accept(value);
            }

            @Override
            public void failed(Throwable error) {
            }
        };
    }

    interface Progress<T> extends Receiver<Loader<? extends T>> {
        void running(@Nullable T currentValue);
        void done(T value);

        default void failed(Throwable error) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            else {
                throw new RuntimeException(error);
            }
        }

        @Override
        default void accept(@Nullable Loader<? extends T> loader) {
            if (loader != null) {
                loader.select(this);
            }
        }
    }
}

