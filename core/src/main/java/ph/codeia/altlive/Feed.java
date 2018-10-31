package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.Nullable;

/**
 * An object that causes a {@link Live} object to update upon receiving a value.
 *
 * @param <T> The type of the input.
 */
public interface Feed<T> {
    /**
     * Causes some dependent {@link Live} object to notify its active observers
     * in a particular thread.
     *
     * <p> The type parameters of {@code Feed} and the dependent {@code Live} do
     * not have to be the same.
     */
    void postValue(@Nullable T t);
}

