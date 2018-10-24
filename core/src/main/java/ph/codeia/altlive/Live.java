package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.Nullable;

/**
 * An object that emits values to observers associated with a lifecycle.
 *
 * <p> This interface is patterned after the public interface of AAC's
 * {@code MutableLiveData} class with a few differences:
 *
 * <ul><li> The input and output sides are split. This interface represents
 * the output side and {@link Feed} represents the input side.
 * <li> {@code getValue()} is excluded from the interface because code relying
 * on that is likely to be using observables incorrectly. It still exists in
 * {@link LiveField} though and is public as well.
 * <li> The methods for querying the presence of observers are gone. I think
 * they have no value outside of tests, and tests relying on them in order to
 * verify expected behavior are badly written.
 * <li> Added default methods for listening to a single update. The observer is
 * removed automatically after being invoked once.</ul>
 *
 * @param <T> The type of the observable data
 */
public interface Live<T> {
    /**
     * Registers an observer that will be updated for as long as the owner is
     * in the active state.
     *
     * <p> An observer can be associated with only one lifecycle owner. It is
     * up to the implementation to define the bounds of the active state. When
     * the owner enters the {@code DESTROYED} state, the observer and owner are
     * automatically removed.
     */
    void observe(LifecycleOwner owner, Receiver<? super T> receiver);

    /**
     * Removes all registered observers associated with this lifecycle owner.
     *
     * <p> The given owner and all observers associated with it are dropped from
     * the registry.
     */
    void removeObservers(LifecycleOwner owner);

    /**
     * Removes a registered observer.
     *
     * <p> Removing an observer does not necessarily remove its associated owner
     * from the live object's registry since an owner might be associated with
     * other observers in the same live object. When an owner becomes orphaned
     * as a result of this call, the reference to the owner is dropped.
     */
    void removeObserver(Receiver<? super T> receiver);

    /**
     * Associates this observer with the {@link Immortal} instance.
     */
    default void observeForever(Receiver<? super T> receiver) {
        observe(Immortal.INSTANCE, receiver);
    }

    /**
     * Associates an observer that will be invoked at most once with the
     * {@link Immortal} instance.
     */
    default void observeOnce(Receiver<? super T> receiver) {
        observeOnce(Immortal.INSTANCE, receiver);
    }

    /**
     * Registers an owner*observer tuple that will be invoked at most once.
     */
    default void observeOnce(LifecycleOwner owner, Receiver<? super T> receiver) {
        observe(owner, new Receiver<T>() {
            @Override
            public void accept(@Nullable T t) {
                receiver.accept(t);
                removeObserver(this);
            }
        });
    }
}

