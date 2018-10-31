package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

public class LiveLoader<T> implements Live<Loader<T>>, Feed<Try<T>> {

    private final LiveField<Loader<T>> delegate;
    private volatile T value;

    public LiveLoader(LiveField<Loader<T>> delegate) {
        this.delegate = delegate;
    }

    public LiveLoader(LiveField.Builder builder) {
        this(builder.build());
    }

    public LiveLoader() {
        this(new LiveField.Builder());
    }

    @Override
    public void postValue(@Nullable Try<T> loader) {
        if (loader == null) {
            delegate.postValue(null);
            return;
        }
        T oldValue = value;
        delegate.postValue(e -> e.running(oldValue));
        loader.select(new Try.Case<T>() {
            @Override
            public void ok(T t) {
                value = t;
                delegate.postValue(e -> e.done(t));
            }

            @Override
            public void error(@NonNull Throwable t) {
                delegate.postValue(e -> e.failed(t));
            }
        });
    }

    @Override
    public void observe(
            LifecycleOwner owner,
            Receiver<? super Loader<T>> receiver
    ) {
        delegate.observe(owner, receiver);
    }

    @Override
    public void removeObservers(LifecycleOwner owner) {
        delegate.removeObservers(owner);
    }

    @Override
    public void removeObserver(Receiver<? super Loader<T>> receiver) {
        delegate.removeObserver(receiver);
    }
}
