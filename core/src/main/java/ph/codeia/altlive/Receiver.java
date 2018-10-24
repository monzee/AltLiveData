package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A function that observes updates to a live object.
 *
 * @param <T> The type of the object being observed
 */
public interface Receiver<T> {
    /**
     * Called when a live object is updated.
     */
    void accept(@Nullable T t);

    /**
     * Non-null variant for use in {@link #forSome(NullSafe)}.
     *
     * @param <T> The type of the object being observed
     */
    interface NullSafe<T> {
        void accept(@NonNull T t);
    }

    /**
     * Wrapper for observer lambdas to elide null-checking while making the
     * linter happy.
     *
     * @param delegate The non-null observer function
     * @param <T> The type of the object being observed
     * @return a regular (nullable) observer
     */
    static <T> Receiver<T> forSome(NullSafe<? super T> delegate) {
        return t -> {
            if (t != null) {
                delegate.accept(t);
            }
        };
    }
}
