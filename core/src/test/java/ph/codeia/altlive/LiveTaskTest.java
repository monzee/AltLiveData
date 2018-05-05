package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.Nullable;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ph.codeia.altlive.transform.Execute;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class LiveTaskTest {
    private static final ExecutorService BG = Executors.newSingleThreadExecutor();
    private static final ExecutorService FG = Executors.newSingleThreadExecutor();
    private static final ThreadLocal<String> S = new ThreadLocal<>();

    static {
        FG.execute(() -> S.set("foreground"));
        BG.execute(() -> S.set("background"));
    }

    @AfterClass
    public static void tearDown() {
        BG.shutdown();
        FG.shutdown();
    }

    @Test(timeout = 1000, expected = ArithmeticException.class)
    public void strawman() throws InterruptedException {
        LiveField<Task<Integer, Integer>> backingField = new LiveField<>(FG);
        LiveTask<Integer, Integer> divBy = new LiveTask<>(
                backingField,
                denom -> Try.of(() -> 12 / denom).pipe(Execute.on(BG))
        );
        CountDownLatch done = new CountDownLatch(10);
        AtomicInteger prev = new AtomicInteger(-1);
        divBy.observeForever(new Task.Progress<Integer, Integer>() {
            @Override
            public void running(Integer key, @Nullable Integer currentValue) {
                if (currentValue != null) {
                    assertEquals(prev.get(), currentValue.intValue());
                }
                done.countDown();
            }

            @Override
            public void done(Integer key, Integer value) {
                assertNotNull(value);
                assertEquals(12 / key, value.intValue());
                prev.set(value);
                done.countDown();
            }

            @Override
            public void failed(Integer key, Throwable error) {
                done.countDown();
            }
        });
        divBy.postValue(6);
        divBy.postValue(4);
        divBy.postValue(3);
        divBy.postValue(2);
        divBy.postValue(0);
        done.await();
        backingField.getValue().toTry().unwrap();
    }

    @Test(timeout = 1000)
    public void progress_callbacks_are_called_in_the_target_thread() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(3);
        LiveTask<Object, ?> task = new LiveTask<>(
                new LiveField<>(FG),
                o -> Try.of(() -> {
                    assertEquals("background", S.get());
                    done.countDown();
                    return null;
                }).pipe(Execute.on(BG))
        );
        task.observeForever(new Task.Progress<Object, Object>() {
            @Override
            public void running(Object key, @Nullable Object currentValue) {
                assertEquals("foreground", S.get());
                done.countDown();
            }

            @Override
            public void done(Object key, Object value) {
                assertEquals("foreground", S.get());
                done.countDown();
            }
        });

        task.postValue(new Object());
        done.await();
    }

    @Test
    public void progress_running_callback_is_invoked_before_the_task() {
        AtomicInteger counter = new AtomicInteger(0);
        LiveTask<Object, Void> task = new LiveTask<>(o -> {
            assertEquals(2, counter.incrementAndGet());
            return Try.just(null);
        });
        task.observeForever(new Task.Progress<Object, Void>() {
            @Override
            public void running(Object key, @Nullable Void currentValue) {
                assertEquals(1, counter.incrementAndGet());
            }

            @Override
            public void done(Object key, Void value) {
                counter.incrementAndGet();
            }
        });

        task.postValue(new Object());
        assertEquals(3, counter.get());
    }

    @Test
    public void posting_null_does_not_invoke_the_task_and_callbacks() {
        AtomicInteger counter = new AtomicInteger(0);
        LiveTask<Object, Void> task = new LiveTask<>(o -> {
            counter.incrementAndGet();
            return Try.just(null);
        });
        task.observeForever(new Task.Progress<Object, Void>() {
            @Override
            public void running(Object key, @Nullable Void currentValue) {
                assertNotNull(key);
                counter.incrementAndGet();
            }

            @Override
            public void done(Object key, Void value) {
                assertNotNull(key);
                counter.incrementAndGet();
            }
        });

        task.postValue(new Object());
        assertEquals(3, counter.get());
        task.postValue(null);
        assertEquals(3, counter.get());
        task.postValue(new Object());
        assertEquals(6, counter.get());
    }

    @Test
    public void subsequent_observers_receive_the_progress_of_the_previous_post() {
        LiveTask<String, String> task = new LiveTask<>(Try::just);
        AtomicReference<String> primary = new AtomicReference<>();
        task.observeForever(Task.whenDone(primary::set));
        task.postValue("foo");
        assertEquals("foo", primary.get());

        AtomicReference<String> secondary = new AtomicReference<>();
        task.observeForever(Task.whenDone(secondary::set));
        assertEquals("foo", secondary.get());
    }

    @Test
    public void posting_null_clears_the_progress_of_the_previous_post() {
        LiveTask<String, String> task = new LiveTask<>(Try::just);
        AtomicReference<String> primary = new AtomicReference<>();
        task.observeForever(Task.whenDone(primary::set));
        task.postValue("foo");
        assertEquals("foo", primary.get());

        task.postValue(null);
        AtomicReference<String> secondary = new AtomicReference<>();
        task.observeForever(Task.whenDone(secondary::set));
        assertNull(secondary.get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void default_error_handler_crashes_the_current_thread() {
        LiveTask<Object, ?> task = new LiveTask<>(o ->
                Try.raise(new UnsupportedOperationException()));
        task.observeForever(new Task.Progress<Object, Object>() {
            @Override
            public void running(Object key, @Nullable Object currentValue) {
            }

            @Override
            public void done(Object key, Object value) {
                fail("unreachable");
            }
        });
        task.postValue(new Object());
    }

    @Test(timeout = 1000)
    public void threaded_default_error_handler_crashes_the_target_thread()
    throws InterruptedException {
        CountDownLatch setup = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        ExecutorService main = Executors.newSingleThreadExecutor();
        try {
            main.execute(() -> {
                Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                    assertThat(e, instanceOf(UnsupportedOperationException.class));
                    done.countDown();
                });
                setup.countDown();
            });
            LiveTask<Object, ?> task = new LiveTask<>(
                    new LiveField<>(main),
                    o -> Try.raise(new UnsupportedOperationException()).pipe(Execute.on(BG))
            );
            task.observeForever(new Task.Progress<Object, Object>() {
                @Override
                public void running(Object key, @Nullable Object currentValue) {
                }

                @Override
                public void done(Object key, Object value) {
                    fail("unreachable");
                }
            });

            setup.await();
            task.postValue(new Object());
            done.await();
        }
        finally {
            main.shutdown();
        }
    }
}

