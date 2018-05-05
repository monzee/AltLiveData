package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
            Observer<? super Loader<T>> observer
    ) {
        delegate.observe(owner, observer);
    }

    @Override
    public void removeObservers(LifecycleOwner owner) {
        delegate.removeObservers(owner);
    }

    @Override
    public void removeObserver(Observer<? super Loader<T>> observer) {
        delegate.removeObserver(observer);
    }
}
