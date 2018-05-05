package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.Nullable;

/**
 * A function that observes updates to a live object.
 *
 * @param <T> The type of the object being observed
 */
public interface Observer<T> {
    /**
     * Called when a live object is updated.
     */
    void onChanged(@Nullable T t);
}
