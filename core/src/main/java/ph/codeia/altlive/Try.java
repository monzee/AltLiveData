package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a computation that might fail.
 *
 * @param <T> The type of the value being computed.
 */
public interface Try<T> {
    /**
     * "Pattern matches" the result of the computation.
     */
    void select(Case<? super T> continuation);

    /**
     * Performs the computation and returns the value.
     *
     * <p> Blocks the current thread until a value is completed or an error is
     * thrown. Might cause deadlocks.
     */
    default T unwrap() {
        return new Case<T>() {
            final Lock lock = new ReentrantLock();
            final Condition done = lock.newCondition();
            boolean reallyDone;
            T result;
            Throwable error;

            {
                select(this);
                lock.lock();
                try {
                    while (!reallyDone) {
                        done.awaitUninterruptibly();
                    }
                    if (error != null) {
                        if (error instanceof RuntimeException) {
                            throw (RuntimeException) error;
                        }
                        else {
                            throw new RuntimeException(error);
                        }
                    }
                }
                finally {
                    lock.unlock();
                }
            }

            @Override
            public void ok(T t) {
                lock.lock();
                result = t;
                reallyDone = true;
                done.signalAll();
                lock.unlock();
            }

            @Override
            public void error(@NonNull Throwable t) {
                lock.lock();
                error = t;
                reallyDone = true;
                done.signalAll();
                lock.unlock();
            }
        }.result;
    }

    /**
     * Transforms the value of the success branch.
     *
     * @param f The transform function. Not invoked when in the error case.
     * @param <U> The type of the transformed value.
     */
    default <U> Try<U> map(Function<? super T, ? extends U> f) {
        return continuation -> select(new Case<T>() {
            @Override
            public void ok(T t) {
                continuation.ok(f.apply(t));
            }

            @Override
            public void error(@NonNull Throwable t) {
                continuation.error(t);
            }
        });
    }

    /**
     * Uses the success value to perform another computation.
     *
     * @param f A function that produces a computation from the success value.
     *          Not invoked when in the error case.
     * @param <U> The type of the result of the downstream computation
     */
    default <U> Try<U> flatMap(Function<? super T, ? extends Try<U>> f) {
        return continuation -> select(new Case<T>() {
            @Override
            public void ok(T t) {
                f.apply(t).select(continuation);
            }

            @Override
            public void error(@NonNull Throwable t) {
                continuation.error(t);
            }
        });
    }

    /**
     * Chains this computation into another computation.
     *
     * <p> Like {@link #flatMap(Function)} but is more general. Both {@link #flatMap}
     * and {@link #map} can be defined in terms of this method. Those two
     * are very commonly used so they are hardcoded into the interface.
     *
     * @param <U> The type of the success value of the decorator
     * @param <R> The actual type of the decorator
     * @see ph.codeia.altlive.transform.Memoize for an example of a decorator.
     */
    default <U, R extends Try<U>> R pipe(Function<Try<T>, R> transformer) {
        return transformer.apply(this);
    }

    /**
     * A computation that always succeeds.
     *
     * @param <T> The type of the value
     */
    static <T> Try<T> just(T t) {
        return continuation -> continuation.ok(t);
    }

    /**
     * A computation that always fails.
     *
     * @param <T> The type of the success value
     */
    static <T> Try<T> raise(Throwable t) {
        if (t == null) {
            throw new NullPointerException("error can't be null");
        }
        return continuation -> continuation.error(t);
    }

    /**
     * Converts a {@link Callable} into a {@link Try}.
     *
     * @param <T> The return type of the callable
     */
    static <T> Try<T> of(Callable<T> block) {
        return continuation -> {
            try {
                continuation.ok(block.call());
            }
            catch (Exception e) {
                continuation.error(e);
            }
        };
    }

    /**
     * Converts a {@link Runnable} into a void {@link Try}.
     */
    static Try<Void> of(Runnable block) {
        return continuation -> {
            try {
                block.run();
                continuation.ok(null);
            }
            catch (RuntimeException e) {
                continuation.error(e);
            }
        };
    }

    /**
     * The "pattern" interface used to unwrap the result of the computation.
     *
     * @param <T> The type of the success value.
     */
    interface Case<T> extends Observer<Try<? extends T>> {
        /**
         * Called when the computation succeeds.
         */
        void ok(T t);

        /**
         * Called when the computation fails.
         */
        void error(@NonNull Throwable t);

        @Override
        default void onChanged(@Nullable Try<? extends T> result) {
            if (result != null) {
                result.select(this);
            }
        }
    }
}

