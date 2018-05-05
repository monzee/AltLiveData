package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.LifecycleOwner;

import org.junit.Test;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LeakTest {

    private static Reference<?> gc(ReferenceQueue<?> q) throws InterruptedException {
        assertNull(q.poll());
        Reference<?> ref = null;
        for (; ref == null; ref = q.remove(100)) {
            System.gc();
        }
        return ref;
    }

    @Test(timeout = 1000)
    public void gc_works() throws InterruptedException {
        Object p = new Object();
        ReferenceQueue<Object> q = new ReferenceQueue<>();
        PhantomReference<Object> ref = new PhantomReference<>(p, q);
        p = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void owner_is_dropped_when_owner_is_destroyed() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        field.observe(owner, _v -> counter.incrementAndGet());
        ReferenceQueue<LifecycleOwner> q = new ReferenceQueue<>();
        PhantomReference<LifecycleOwner> ref = new PhantomReference<>(owner, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        owner.regress();
        owner.regress();
        owner.regress();
        owner = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void observer_is_dropped_when_owner_is_destroyed() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        Observer<Void> obs = _v -> counter.incrementAndGet();
        field.observe(owner, obs);
        ReferenceQueue<Observer> q = new ReferenceQueue<>();
        PhantomReference<Observer> ref = new PhantomReference<>(obs, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        owner.regress();
        owner.regress();
        owner.regress();
        obs = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void observer_is_dropped_when_removed() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        Observer<Void> obs = _v -> counter.incrementAndGet();
        field.observe(owner, obs);
        ReferenceQueue<Observer> q = new ReferenceQueue<>();
        PhantomReference<Observer> ref = new PhantomReference<>(obs, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        field.removeObserver(obs);
        obs = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void observer_is_dropped_when_removeObservers_is_called() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        Observer<Void> obs = _v -> counter.incrementAndGet();
        field.observe(owner, obs);
        ReferenceQueue<Observer> q = new ReferenceQueue<>();
        PhantomReference<Observer> ref = new PhantomReference<>(obs, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        field.removeObservers(owner);
        obs = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void owner_is_dropped_when_removeObservers_is_called() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        field.observe(owner, _v -> counter.incrementAndGet());
        ReferenceQueue<LifecycleOwner> q = new ReferenceQueue<>();
        PhantomReference<LifecycleOwner> ref = new PhantomReference<>(owner, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        field.removeObservers(owner);
        owner = null;
        assertNotNull(gc(q));
    }

    @Test(timeout = 1000)
    public void owner_is_dropped_when_the_last_observer_is_removed() throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        Observer<Void> obs = _v -> counter.incrementAndGet();
        field.observe(owner, obs);
        ReferenceQueue<LifecycleOwner> q = new ReferenceQueue<>();
        PhantomReference<LifecycleOwner> ref = new PhantomReference<>(owner, q);

        assertEquals(0, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
        field.removeObserver(obs);
        owner = null;
        assertNotNull(gc(q));
    }
}
