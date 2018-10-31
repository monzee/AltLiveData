package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import ph.codeia.altlive.Try;

public class Memoize<T> implements Try<T> {
    private final Try<T> source;
    private T value;
    private Throwable error;
    private boolean isDone = false;
    private long timestamp;

    @Transformer
    public Memoize(Try<T> source) {
        this.source = source;
    }

    public synchronized void clear() {
        value = null;
        error = null;
        isDone = false;
    }

    public boolean hasValue() {
        return isDone;
    }

    public boolean isOlderThan(long millis) {
        return !isDone || System.currentTimeMillis() - timestamp > millis;
    }

    @Override
    public void select(Case<? super T> continuation) {
        if (isDone) {
            resume(continuation);
            return;
        }
        source.select(new Case<T>() {
            @Override
            public void ok(T t) {
                synchronized (Memoize.this) {
                    if (!isDone) {
                        value = t;
                        timestamp = System.currentTimeMillis();
                        isDone = true;
                    }
                }
                resume(continuation);
            }

            @Override
            public void error(@NonNull Throwable t) {
                synchronized (Memoize.this) {
                    if (!isDone) {
                        error = t;
                        timestamp = System.currentTimeMillis();
                        isDone = true;
                        continuation.error(t);
                    }
                }
                resume(continuation);
            }
        });
    }

    private void resume(Case<? super T> continuation) {
        if (error == null) {
            continuation.ok(value);
        }
        else {
            continuation.error(error);
        }
    }
}

