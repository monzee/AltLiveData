package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

/**
 * A {@link LifecycleOwner} that is always in the {@link Lifecycle.State#RESUMED} state.
 *
 * <p> Added observers are simply brought to the resumed state by triggering
 * {@code ON_CREATE}, {@code ON_START}, and {@code ON_RESUME} consecutively.
 * Does not maintain a registry of added observers, so calling {@code addObserver()}
 * multiple times will also trigger the callbacks multiple times. Calling
 * {@code removeObserver()} does nothing.
 */
public enum Immortal implements LifecycleOwner {
    INSTANCE;

    private final Lifecycle alwaysRunning = new Lifecycle() {
        @Override
        public void addObserver(@NonNull LifecycleObserver observer) {
            LifecycleOwner owner = Immortal.this;
            if (observer instanceof DefaultLifecycleObserver) {
                DefaultLifecycleObserver o = (DefaultLifecycleObserver) observer;
                o.onCreate(owner);
                o.onStart(owner);
                o.onResume(owner);
            }
            else if (observer instanceof GenericLifecycleObserver) {
                GenericLifecycleObserver o = (GenericLifecycleObserver) observer;
                o.onStateChanged(owner, Event.ON_CREATE);
                o.onStateChanged(owner, Event.ON_START);
                o.onStateChanged(owner, Event.ON_RESUME);
            }
        }

        @Override
        public void removeObserver(@NonNull LifecycleObserver observer) {
        }

        @NonNull
        @Override
        public State getCurrentState() {
            return State.RESUMED;
        }
    };

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return alwaysRunning;
    }
}

