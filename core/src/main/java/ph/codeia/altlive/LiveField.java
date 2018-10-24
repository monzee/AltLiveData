package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * A {@link Live} object whose value is externally changeable.
 *
 * <p> This class is supposed to be a drop-in replacement for AAC's
 * {@code MutableLiveData}. It's not meant to be extended though but rather
 * decorated by another {@link Live} implementation that simply delegates to
 * this class.
 *
 * <p> Observers with active owners are notified when a value is set through
 * {@link #setValue(Object)} or {@link #postValue(Object)}. The active period
 * of an owner can be specified through {@link Builder#activate(Lifecycle.Event)}
 * and {@link Builder#deactivate(Lifecycle.Event)}. By default, this is the period
 * between {@code ON_RESUME} and {@code ON_PAUSE} (i.e. the {@code RESUMED}
 * state). This is different from AAC's {@code LiveData}, which is hard-coded
 * to be active during the {@code STARTED} state.
 *
 * <p> A live field can be instantiated with the sticky flag set. A sticky field
 * notifies an observer with the current value every time its owner enters the
 * active state. This is the default behavior which is similar to but not quite
 * the same<sup>(1)</sup> as {@code LiveData}. When a field is non-sticky, the
 * observers will only be notified if the value is set while the owner is active.
 * An observer will never be notified by a non-sticky field during registration
 * or owner activation. This is useful for transient observable state or events
 * like logging and toasts.
 *
 * <p> Observers are notified in a thread owned by its executor when the value
 * is set through {@link #postValue(Object)}. The default executor calls them
 * in the same thread as the caller, which is probably not what you want in
 * Android. Use the {@code AndroidLive} facade to instantiate live fields to
 * ensure that they are posted to the UI thread.
 *
 * <p> Owner references are strongly held until 1) the owner is destroyed through
 * their lifecycle hook or 2) there are no observers associated with it anymore.
 * Observers are similarly strongly held until 1) they are removed or 2) their
 * owner is destroyed.
 *
 * <p><sup>(1)</sup> If I understand correctly, {@code LiveData} guarantees that
 * one call to {@code postValue(t)} will cause at most one call to an observer's
 * {@code onChanged(t)}. So if a value posted is while an observer's owner is
 * active, the observer will be invoked once with that value. Later on when the
 * owner exits the active state and reenters it before it is destroyed and no
 * new value is posted meanwhile, the observer will not be called again with the
 * current value. A sticky field will call the observer in that scenario.
 *
 * @param <T> The type of the observable data
 */
public class LiveField<T> implements Live<T>, Feed<T>, DefaultLifecycleObserver {

    /**
     * Mutable builder to configure a {@link LiveField} instance.
     */
    public static class Builder {
        private Executor executor = Runnable::run;
        private Lifecycle.Event activator = Lifecycle.Event.ON_RESUME;
        private Lifecycle.Event deactivator = Lifecycle.Event.ON_PAUSE;
        private boolean isSticky = true;

        /**
         * Sets the thread where the observers are called when the value is set.
         */
        public Builder postOn(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets the event which marks the start of the lifecycle's active state.
         *
         * <p> Defaults to {@code ON_RESUME} when not configured.
         *
         * @param activator Must be one of {@code ON_CREATE}, {@code ON_START} or
         *                  {@code ON_PAUSE}. Throws an {@link IllegalArgumentException}
         *                  otherwise.
         */
        public Builder activate(Lifecycle.Event activator) {
            switch (activator) {
                case ON_CREATE:
                case ON_START:
                case ON_RESUME:
                    this.activator = activator;
                    return this;
                default:
                    throw new IllegalArgumentException("Expecting ON_CREATE, ON_START or ON_RESUME");
            }
        }

        /**
         * Sets the event which marks the end of the lifecycle's active state.
         *
         * <p> No observer will be invoked until the owner reenters the active
         * state.
         *
         * <p> Defaults to {@code ON_PAUSE} when not configured.
         *
         * @param deactivator Must be one of {@code ON_DESTROY}, {@code ON_STOP}
         *                    or {@code ON_PAUSE}. Throws an {@link IllegalArgumentException}
         *                    otherwise.
         */
        public Builder deactivate(Lifecycle.Event deactivator) {
            switch (deactivator) {
                case ON_DESTROY:
                case ON_STOP:
                case ON_PAUSE:
                    this.deactivator = deactivator;
                    return this;
                default:
                    throw new IllegalArgumentException("Expecting ON_DESTROY, ON_STOP or ON_PAUSE");
            }
        }

        /**
         * Sets the sticky mode of the live field.
         *
         * <p> A sticky field notifies its observers whenever their owners enter
         * the active state and a value is available. A non-sticky field does
         * not notify its observers when a value arrives while the owner is in
         * the inactive state.
         *
         * <p> Live fields are sticky by default.
         */
        public Builder sticky(boolean isSticky) {
            this.isSticky = isSticky;
            return this;
        }

        /**
         * Returns a new field with the current configuration.
         *
         * <p> The current configuration is copied into the new field instance,
         * so further mutation of this builder will not affect previously built
         * live fields.
         */
        public <T> LiveField<T> build() {
            return new LiveField<>(this);
        }

        /**
         * Returns a new field with the current configuration and an initial value.
         *
         * @see #build()
         */
        public <T> LiveField<T> build(T seed) {
            LiveField<T> result = build();
            result.setValue(seed);
            return result;
        }
    }

    private final Map<LifecycleOwner, OwnerMeta<T>> metaByOwner = new HashMap<>();
    private final Map<Receiver<? super T>, LifecycleOwner> ownerByObserver = new HashMap<>();
    private final Executor executor;
    private final Lifecycle.Event activator;
    private final Lifecycle.Event deactivator;
    private final boolean isSticky;
    private boolean hasValue;
    private T value;

    private LiveField(Builder builder) {
        executor = builder.executor;
        activator = builder.activator;
        deactivator = builder.deactivator;
        isSticky = builder.isSticky;
    }

    /**
     * Instantiates a live field with the default builder options.
     *
     * @see Builder
     */
    public LiveField() {
        this(new Builder());
    }

    /**
     * Instantiates a live field that calls observers in the context of this
     * executor.
     *
     * <p> Uses the default builder options apart from the executor.
     *
     * @see Builder
     */
    public LiveField(Executor executor) {
        this(new Builder().postOn(executor));
    }

    /**
     * Removes the current value.
     *
     * <p> No observer will be invoked until a new value is set.
     */
    public synchronized void clear() {
        value = null;
        hasValue = false;
    }

    /**
     * Returns the current value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the current value.
     *
     * <p> Observers attached to an active owner will be invoked with this
     * value in the current thread.
     */
    public void setValue(T t) {
        synchronized (this) {
            value = t;
            hasValue = true;
        }
        for (OwnerMeta<T> meta : metaByOwner.values()) {
            meta.notifyReceivers(t);
        }
    }

    @Override
    public void postValue(@Nullable T t) {
        executor.execute(() -> setValue(t));
    }

    /**
     * {@inheritDoc}
     *
     * <p> Throws an {@link IllegalArgumentException} when an observer is re-
     * registered with a different owner. Does nothing when an observer is re-
     * registered with the same owner.
     */
    @Override
    public void observe(LifecycleOwner owner, Receiver<? super T> receiver) {
        Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        LifecycleOwner existingOwner = ownerByObserver.get(receiver);
        if (existingOwner == null) synchronized (this) {
            existingOwner = ownerByObserver.get(receiver);
            if (existingOwner == null) {
                existingOwner = owner;
                ownerByObserver.put(receiver, owner);
            }
        }
        if (existingOwner != owner) {
            throw new IllegalArgumentException("An observer can only be associated with one owner");
        }

        OwnerMeta<T> meta = metaByOwner.get(owner);
        if (meta != null && meta.receivers.contains(receiver)) {
            return;
        }
        synchronized (this) {
            meta = metaByOwner.get(owner);
            if (meta == null) {
                meta = new OwnerMeta<>();
                meta.receivers.add(receiver);
                metaByOwner.put(owner, meta);
                lifecycle.addObserver(this);
            }
            else if (!meta.receivers.contains(receiver)){
                meta.receivers.add(receiver);
                if (isSticky && hasValue && meta.active) {
                    receiver.accept(value);
                }
            }
        }
    }

    @Override
    public synchronized void removeObservers(LifecycleOwner owner) {
        OwnerMeta<T> meta = metaByOwner.remove(owner);
        if (meta != null) {
            owner.getLifecycle().removeObserver(this);
            for (Receiver<? super T> receiver : meta.receivers) {
                ownerByObserver.remove(receiver);
            }
        }
    }

    @Override
    public synchronized void removeObserver(Receiver<? super T> receiver) {
        LifecycleOwner owner = ownerByObserver.remove(receiver);
        if (owner != null) {
            OwnerMeta<T> meta = metaByOwner.get(owner);
            meta.receivers.remove(receiver);
            if (meta.receivers.isEmpty()) {
                metaByOwner.remove(owner);
                owner.getLifecycle().removeObserver(this);
            }
        }
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        maybeActivate(Lifecycle.Event.ON_CREATE, owner);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        maybeActivate(Lifecycle.Event.ON_START, owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        maybeActivate(Lifecycle.Event.ON_RESUME, owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        maybeDeactivate(Lifecycle.Event.ON_PAUSE, owner);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        maybeDeactivate(Lifecycle.Event.ON_STOP, owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        // no point in deactivating since it's being removed anyway
        owner.getLifecycle().removeObserver(this);
        removeObservers(owner);
    }

    private void maybeActivate(Lifecycle.Event event, LifecycleOwner owner) {
        if (activator == event) {
            OwnerMeta<T> meta = metaByOwner.get(owner);
            meta.active = true;
            if (isSticky && hasValue) {
                meta.notifyReceivers(value);
            }
        }
    }

    private void maybeDeactivate(Lifecycle.Event event, LifecycleOwner owner) {
        if (deactivator == event) {
            metaByOwner.get(owner).active = false;
        }
    }

    private static class OwnerMeta<T> {
        final List<Receiver<? super T>> receivers = new CopyOnWriteArrayList<>();
        volatile boolean active = false;

        void notifyReceivers(T value) {
            if (active) for (Receiver<? super T> receiver : receivers) {
                receiver.accept(value);
            }
        }
    }
}

