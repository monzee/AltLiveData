package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.Callable;

/**
 * Interface for one-argument unchecked functions that return a value.
 *
 * @param <D> The domain of the function
 * @param <R> The range of the function
 */
public interface Function<D, R> {
    /**
     * Calls the function.
     */
    R apply(D d);

    /**
     * Variant for checked functions.
     *
     * @param <D> The domain of the function
     * @param <R> The range of the function
     */
    interface Checked<D, R> {
        /**
         * Calls the checked function.
         */
        R apply(D d) throws Throwable;
    }

    /**
     * Composes this function with another function whose domain is the same
     * as the range of this function.
     *
     * @param f The function to compose
     * @param <S> The range of the passed function and the resulting composition
     * @return the result of applying the second function to the result of this
     * function applied to the input of the composed function.
     */
    default <S> Function<D, S> then(Function<? super R, ? extends S> f) {
        return d -> f.apply(apply(d));
    }

    /**
     * The identity function.
     *
     * @param <T> The type of the input and output
     * @return a function that simply returns whatever was passed into it.
     */
    static <T> Function<T, T> id() {
        return t -> t;
    }

    /**
     * Converts a callable into a function.
     *
     * @param block a function that produces a value
     * @param <T> the range of the callable
     * @return a function that takes null and returns the result of the callable
     */
    static <T> Function<Void, Try<T>> of(Callable<? extends T> block) {
        return _v -> continuation -> {
            try {
                continuation.ok(block.call());
            }
            catch (Exception e) {
                continuation.error(e);
            }
        };
    }

    /**
     * Converts a runnable into a function.
     *
     * @param block a procedure that produces a side effect.
     * @return a function that takes null and returns null
     */
    static Function<Void, Try<Void>> of(Runnable block) {
        return _v -> continuation -> {
            try {
                block.run();
                continuation.ok(null);
            }
            catch (RuntimeException e) {
                continuation.error(e);
            }
        };
    }
}
