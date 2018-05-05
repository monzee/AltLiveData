package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;

import ph.codeia.altlive.Try;

public class Memoize<T> implements Try<T> {
    private final Try<T> source;
    private T value;
    private Throwable error;
    private boolean done;

    public Memoize(Try<T> source) {
        this.source = source;
    }

    public synchronized void clear() {
        value = null;
        error = null;
        done = false;
    }

    @Override
    public void select(Case<? super T> continuation) {
        if (!done) synchronized (this) {
            if (!done) {
                source.select(new Case<T>() {
                    @Override
                    public void ok(T t) {
                        value = t;
                        done = true;
                        continuation.ok(t);
                    }

                    @Override
                    public void error(@NonNull Throwable t) {
                        error = t;
                        done = true;
                        continuation.error(t);
                    }
                });
            }
        }
        else if (error == null) {
            continuation.ok(value);
        }
        else {
            continuation.error(error);
        }
    }
}

