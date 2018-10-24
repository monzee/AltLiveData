package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ph.codeia.altlive.Try;

import static org.junit.Assert.*;

public class TimeLimitTest {
    private static final ExecutorService E = Executors.newSingleThreadExecutor();
    private static final ThreadLocal<String> S = new ThreadLocal<>();

    static {
        E.execute(() -> S.set("worker"));
    }

    @AfterClass
    public static void tearDown() {
        E.shutdown();
    }

    @Test(expected = TimeoutException.class)
    public void strawman() throws Throwable {
        Execute.on(E, () -> {
            Thread.sleep(32);
            return null;
        }).pipe(TimeLimit.of(16)).unwrapChecked();
    }

    @Test
    public void timeout_drift_is_acceptable() throws Throwable {
        long start = System.currentTimeMillis();
        long target = 50;
        try {
            Execute.on(E, () -> {
                Thread.sleep(target * 2);
                return null;
            }).pipe(TimeLimit.of(target)).unwrapChecked();
            fail("unreachable");
        }
        catch (TimeoutException e) {
            long drift = Math.abs(System.currentTimeMillis() - start - target);
            assertTrue("less than 35ms drift", drift < 35);
        }
    }

    @Test
    public void continues_normally_if_timeout_is_not_hit() {
        assertEquals(
                "foo",
                Execute.on(E, () -> "foo")
                        .pipe(TimeLimit.of(1000))
                        .unwrap()
        );
    }

    @Test
    public void remains_synchronous_if_timeout_is_not_hit() {
        S.set("main");
        AtomicReference<String> e = new AtomicReference<>();
        Try.just("foo").pipe(TimeLimit.of(1000)).select(new Try.Case<String>() {
            @Override
            public void ok(String t) {
                assertEquals("main", S.get());
                e.set(t);
            }

            @Override
            public void error(@NonNull Throwable t) {
                fail("unreachable");
            }
        });
        assertEquals("foo", e.get());
        S.remove();
    }
}
