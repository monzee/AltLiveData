package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

public class LiveTask<I, O> implements Live<Task<I, O>>, Feed<I> {
    private final LiveField<Task<I, O>> delegate;
    private final Function<I, Try<? extends O>> producer;
    private O value;

    public LiveTask(
            LiveField<Task<I, O>> delegate,
            Function<I, Try<? extends O>> producer
    ) {
        this.delegate = delegate;
        this.producer = producer;
    }

    public LiveTask(
            LiveField.Builder builder,
            Function<I, Try<? extends O>> producer
    ) {
        this(builder.build(), producer);
    }

    public LiveTask(Function<I, Try<? extends O>> producer) {
        this(new LiveField.Builder(), producer);
    }

    @Override
    public void postValue(@Nullable I key) {
        if (key == null) {
            delegate.postValue(null);
            return;
        }
        O oldValue = value;
        delegate.postValue(e -> e.running(key, oldValue));
        try {
            producer.apply(key).select(new Try.Case<O>() {
                @Override
                public void ok(O o) {
                    value = o;
                    delegate.postValue(e -> e.done(key, o));
                }

                @Override
                public void error(@NonNull Throwable t) {
                    delegate.postValue(e -> e.failed(key, t));
                }
            });
        }
        catch (RuntimeException t) {
            delegate.postValue(e -> e.failed(key, t));
        }
    }

    @Override
    public void observe(
            LifecycleOwner owner,
            Receiver<? super Task<I, O>> receiver
    ) {
        delegate.observe(owner, receiver);
    }

    @Override
    public void removeObservers(LifecycleOwner owner) {
        delegate.removeObservers(owner);
    }

    @Override
    public void removeObserver(Receiver<? super Task<I, O>> receiver) {
        delegate.removeObserver(receiver);
    }
}

