package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.Nullable;

public interface Task<I, O> {
    void select(Progress<? super I, ? super O> continuation);

    default Try<O> toTry() {
        return continuation -> select(new Progress<I, O>() {
            @Override
            public void running(I key, @Nullable O currentValue) {
                continuation.ok(currentValue);
            }

            @Override
            public void done(I key, O value) {
                continuation.ok(value);
            }

            @Override
            public void failed(I key, Throwable error) {
                continuation.error(error);
            }
        });
    }

    static <I, O> Progress<I, O> whenDone(Receiver<O> receiver) {
        return new Progress<I, O>() {
            @Override
            public void running(I key, @Nullable O currentValue) {
            }

            @Override
            public void done(I key, O value) {
                receiver.accept(value);
            }

            @Override
            public void failed(I key, Throwable error) {
            }
        };
    }

    static <I, O> Progress<I, O> whenDone(Try.Case<O> continuation) {
        return new Progress<I, O>() {
            @Override
            public void running(I key, @Nullable O currentValue) {
            }

            @Override
            public void done(I key, O value) {
                continuation.ok(value);
            }

            @Override
            public void failed(I key, Throwable error) {
                continuation.error(error);
            }
        };
    }

    interface Progress<I, O> extends Receiver<Task<? extends I, ? extends O>> {
        void running(I key, @Nullable O currentValue);
        void done(I key, O value);

        default void failed(I key, Throwable error) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            else {
                throw new RuntimeException(error);
            }
        }

        @Override
        default void accept(@Nullable Task<? extends I, ? extends O> ioTask) {
            if (ioTask != null) {
                ioTask.select(this);
            }
        }
    }
}

