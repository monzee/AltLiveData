package ph.codeia.altlive;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

class Life extends Lifecycle implements LifecycleOwner {
    private final List<DefaultLifecycleObserver> observers = new CopyOnWriteArrayList<>();
    private State currentState = State.INITIALIZED;

    static Life created() {
        Life owner = new Life();
        owner.currentState = State.CREATED;
        return owner;
    }

    static Life started() {
        Life owner = new Life();
        owner.currentState = State.STARTED;
        return owner;
    }

    static Life resumed() {
        Life owner = new Life();
        owner.currentState = State.RESUMED;
        return owner;
    }

    void progress() {
        switch (currentState) {
            case DESTROYED:
                break;
            case INITIALIZED:
                currentState = State.CREATED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onCreate(this);
                }
                break;
            case CREATED:
                currentState = State.STARTED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onStart(this);
                }
                break;
            case STARTED:
                currentState = State.RESUMED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onResume(this);
                }
                break;
            case RESUMED:
                break;
        }
    }

    void regress() {
        switch (currentState) {
            case DESTROYED:
                break;
            case INITIALIZED:
            case CREATED:
                currentState = State.DESTROYED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onDestroy(this);
                }
                break;
            case STARTED:
                currentState = State.CREATED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onStop(this);
                }
                break;
            case RESUMED:
                currentState = State.STARTED;
                for (DefaultLifecycleObserver observer : observers) {
                    observer.onPause(this);
                }
                break;
        }
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return this;
    }

    @Override
    public void addObserver(@NonNull LifecycleObserver observer) {
        if (observer instanceof DefaultLifecycleObserver) {
            DefaultLifecycleObserver obs = (DefaultLifecycleObserver) observer;
            observers.add(obs);
            switch (currentState) {
                case DESTROYED:
                case INITIALIZED:
                    break;
                case CREATED:
                    obs.onCreate(this);
                    break;
                case STARTED:
                    obs.onCreate(this);
                    obs.onStart(this);
                    break;
                case RESUMED:
                    obs.onCreate(this);
                    obs.onStart(this);
                    obs.onResume(this);
                    break;
            }
        }
    }

    @Override
    public void removeObserver(@NonNull LifecycleObserver observer) {
        if (observer instanceof DefaultLifecycleObserver) {
            observers.remove(observer);
        }
    }

    @NonNull
    @Override
    public State getCurrentState() {
        return currentState;
    }
}
