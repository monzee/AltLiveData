package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;

import static androidx.lifecycle.Lifecycle.Event.*;
import static org.junit.Assert.*;

public class LiveFieldTest {
    private static final ThreadLocal<String> S = new ThreadLocal<>();
    private static final ExecutorService E = Executors.newSingleThreadExecutor();

    @BeforeClass
    public static void setup() {
        E.execute(() -> S.set("inside"));
    }

    @AfterClass
    public static void tearDown() {
        E.shutdown();
    }

    @Test
    public void strawman() {
        LiveField<String> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        Receiver<String> receiver = s -> counter.incrementAndGet();

        field.observeForever(receiver);
        assertEquals(0, counter.get());
        field.setValue("foo");
        assertEquals(1, counter.get());
        assertEquals("foo", field.getValue());

        field.removeObserver(receiver);
        field.setValue("bar");
        assertEquals(1, counter.get());
        assertEquals("bar", field.getValue());
    }

    @Test
    public void does_not_emit_before_activation() {
        AtomicInteger n = new AtomicInteger(0);
        AtomicInteger counter = new AtomicInteger(0);
        Receiver<Integer> obs = i -> {
            counter.incrementAndGet();
            //noinspection ConstantConditions
            n.set(i);
        };
        LiveField.Builder b = new LiveField.Builder();
        LiveField<Integer> create = b.activate(ON_CREATE).build(1234);
        LiveField<Integer> start = b.activate(ON_START).build(2345);
        LiveField<Integer> resume = b.activate(ON_RESUME).build(3456);

        Life owner = new Life();
        resume.observe(owner, obs);
        start.observe(owner, obs);
        create.observe(owner, obs);
        assertEquals(0, n.get());
        assertEquals(0, counter.get());

        owner.progress();
        assertEquals(1234, n.get());
        assertEquals(1, counter.get());

        owner.progress();
        assertEquals(2345, n.get());
        assertEquals(2, counter.get());

        owner.progress();
        assertEquals(3456, n.get());
        assertEquals(3, counter.get());
    }

    @Test
    public void does_not_emit_when_inactive() {
        AtomicInteger counter = new AtomicInteger(0);
        LiveField<Void> field = new LiveField.Builder()
                .activate(ON_START)
                .deactivate(ON_STOP)
                .build();
        Life owner = Life.started();
        field.observe(owner, _v -> counter.incrementAndGet());

        field.setValue(null);
        assertEquals(1, counter.get());
        field.setValue(null);
        assertEquals(2, counter.get());
        owner.regress();  // created
        field.setValue(null);
        assertEquals(2, counter.get());
    }

    @Test
    public void emits_the_value_set_while_inactive_when_activated() {
        AtomicReference<String> value = new AtomicReference<>();
        LiveField<String> field = new LiveField.Builder().build("foo");
        Life owner = Life.resumed();
        field.observe(owner, value::set);

        field.setValue("foo");
        assertEquals("foo", value.get());
        owner.regress();
        field.setValue("bar");
        assertEquals("foo", value.get());
        owner.progress();
        assertEquals("bar", value.get());
    }

    @Test
    public void reemits_when_reactivated() {
        AtomicInteger counter = new AtomicInteger(0);
        LiveField<Void> field = new LiveField.Builder()
                .activate(ON_START)
                .deactivate(ON_STOP)
                .build();
        Life owner = Life.started();
        field.observe(owner, _v -> counter.incrementAndGet());

        field.setValue(null);
        assertEquals(1, counter.get());
        owner.regress();
        assertEquals(1, counter.get());
        owner.progress();
        assertEquals(2, counter.get());
    }

    @Test
    public void observer_can_remove_itself() {
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger(0);
        field.observeForever(new Receiver<Void>() {
            @Override
            public void accept(@Nullable Void aVoid) {
                counter.incrementAndGet();
                field.removeObserver(this);
            }
        });

        field.setValue(null);
        assertEquals(1, counter.get());
        field.setValue(null);
        assertEquals(1, counter.get());
    }

    @Test
    public void observer_can_remove_all_observers_but_does_not_affect_the_current_iteration() {
        LiveField<Void> field = new LiveField<>();
        Life owner = Life.resumed();
        AtomicInteger counter = new AtomicInteger(0);
        field.observe(owner, _v -> counter.incrementAndGet());
        field.observe(owner, _v -> counter.incrementAndGet());
        field.observe(owner, _v -> {
            counter.incrementAndGet();
            field.removeObservers(owner);
        });
        field.observe(owner, _v -> counter.incrementAndGet());
        field.observe(owner, _v -> counter.incrementAndGet());

        field.setValue(null);
        assertEquals(5, counter.get());
        field.setValue(null);
        assertEquals(5, counter.get());
    }

    @Test(timeout = 1000)
    public void observer_is_called_in_the_caller_thread_when_value_is_set()
    throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>(E);
        CountDownLatch done = new CountDownLatch(2);
        field.observe(owner, _v -> {
            assertEquals("outside", S.get());
            done.countDown();
        });

        S.set("outside");
        E.execute(() -> {
            assertNotEquals("outside", S.get());
            done.countDown();
        });
        field.setValue(null);
        done.await();
    }

    @Test(timeout = 1000)
    public void observer_is_called_in_the_target_thread_when_value_is_posted()
    throws InterruptedException {
        Life owner = Life.resumed();
        LiveField<Void> field = new LiveField<>(E);
        CountDownLatch done = new CountDownLatch(2);
        field.observe(owner, _v -> {
            assertEquals("inside", S.get());
            done.countDown();
        });

        assertNull(S.get());
        E.execute(() -> {
            assertEquals("inside", S.get());
            done.countDown();
        });
        field.postValue(null);
        done.await();
    }

    @Test(expected = IllegalArgumentException.class)
    public void does_not_allow_an_observer_to_be_registered_twice_with_different_owners() {
        Life o1 = Life.resumed();
        Life o2 = Life.resumed();
        LiveField<Void> field = new LiveField<>();
        Receiver<Void> obs = o -> {};

        field.observe(o1, obs);
        field.observe(o2, obs);
    }

    @Test
    public void registering_an_observer_twice_does_not_cause_it_to_be_called_twice_per_update() {
        LiveField<Void> field = new LiveField<>();
        AtomicInteger counter = new AtomicInteger();
        Receiver<Void> obs = o -> counter.incrementAndGet();
        field.observeForever(obs);
        field.observeForever(obs);

        field.setValue(null);
        assertEquals(1, counter.get());
    }

    @Test
    public void sticky_field_notifies_observer_on_registration_when_it_has_a_value() {
        LiveField<String> field = new LiveField.Builder().sticky(true).build();
        AtomicReference<String> value = new AtomicReference<>();

        field.setValue("foo");
        field.observeForever(value::set);
        assertEquals("foo", value.get());
    }

    @Test
    public void non_sticky_never_notifies_on_registration_even_when_it_has_a_value() {
        LiveField<String> field = new LiveField.Builder().sticky(false).build();
        AtomicReference<String> value = new AtomicReference<>();

        field.setValue("foo");
        field.observeForever(value::set);
        assertNull(value.get());
    }
}

