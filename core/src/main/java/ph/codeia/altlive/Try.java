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
     * Performs the computation and returns the value or throws a {@code RuntimeException}.
     *
     * <p> Blocks the current thread until a value is produced or an error is
     * raised. <strong>Will deadlock if the computation is scheduled to run on
     * the same thread as the caller</strong>.
     */
    default T unwrap() {
        try {
            return unwrapChecked();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs the computation and returns the value or throws a {@code Throwable}.
     *
     * @see #unwrap()
     */
    default T unwrapChecked() throws Throwable {
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
                        throw error;
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
                try {
                    continuation.ok(f.apply(t));
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
                try {
                    f.apply(t).select(continuation);
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

    /**
     * Chains this computation into another computation.
     *
     * <p> Like {@link #flatMap(Function)} but is more general since it allows
     * you to transform a computation in the error case. Both {@link #flatMap}
     * and {@link #map} could have been defined in terms of this method but
     * since those two are very commonly used, they are hardcoded into the
     * interface.
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
     * @throws NullPointerException when given a null Throwable
     */
    static <T> Try<T> raise(Throwable t) {
        if (t == null) {
            throw new NullPointerException("Error shouldn't be null");
        }
        return continuation -> continuation.error(t);
    }

    /**
     * Converts a {@link Callable} into a {@link Try}.
     *
     * @param <T> The return type of the callable
     */
    static <T> Try<T> of(Callable<? extends T> block) {
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
     * The "pattern" interface used to safely unwrap the result of the computation.
     *
     * <p> Typically, this will only be consumed in the platform's main thread
     * so any runtime exception thrown in either branch would crash the app.
     * However, when you're writing a {@link ph.codeia.altlive.transform.Transformer}
     * that might not necessarily be the case. In fact, it's very likely that
     * they would be called in a worker thread instead. Be mindful of possible
     * throws when taking a user-supplied function (aside from the case callbacks
     * themselves) and make sure to bubble them up to the leaf error case
     * callback. Failure to do so would cause very hard-to-debug problems since
     * there'd likely be no uncaught exception handler installed in the worker
     * thread so the tasks would just fail silently.
     *
     * @param <T> The type of the success value.
     * @see ph.codeia.altlive.transform.Retry for an example of a transform
     * that takes a user-supplied function in order to work.
     */
    interface Case<T> extends Receiver<Try<? extends T>> {
        /**
         * Called when the computation succeeds.
         */
        void ok(T t);

        /**
         * Called when the computation fails.
         */
        void error(@NonNull Throwable t);

        @Override
        default void accept(@Nullable Try<? extends T> result) {
            if (result != null) {
                result.select(this);
            }
        }
    }
}

