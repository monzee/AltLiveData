package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TryTest {
    @Test
    public void callable_is_not_invoked_until_the_try_is_pattern_matched() {
        AtomicInteger counter = new AtomicInteger(0);
        Try<Void> t = Try.of(() -> {
            counter.incrementAndGet();
            return null;
        });

        assertEquals(0, counter.get());
        t.select(new Try.Case<Void>() {
            @Override
            public void ok(Void aVoid) {
            }

            @Override
            public void error(@NonNull Throwable t) {
                fail("unreachable");
            }
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void callable_is_invoked_every_time_the_try_is_pattern_matched() {
        AtomicInteger counter = new AtomicInteger(0);
        Try<?> t = Try.of(() -> {
            counter.incrementAndGet();
            return null;
        });
        Try.Case<Object> noop = new Try.Case<Object>() {
            @Override
            public void ok(Object o) {
            }

            @Override
            public void error(@NonNull Throwable t) {
                fail("unreachable");
            }
        };

        t.select(noop);
        assertEquals(1, counter.get());
        t.select(noop);
        assertEquals(2, counter.get());
        t.select(noop);
        t.select(noop);
        assertEquals(4, counter.get());
    }

    @Test
    public void can_be_unwrapped() {
        Try<String> t = Try.of(() -> "foo");
        assertEquals("foo", t.unwrap());
    }

    @Test(expected = ArithmeticException.class)
    public void throws_when_a_failed_try_is_unwrapped() {
        @SuppressWarnings("NumericOverflow")
        Try<Integer> t = Try.of(() -> 100 / 0);
        t.unwrap();
    }

    @Test
    public void wraps_a_checked_exception_in_a_RuntimeException_when_unwrapped() {
        Try<?> t = Try.of(() -> {
            throw new Exception("foo");
        });
        try {
            t.unwrap();
        }
        catch (RuntimeException e) {
            Throwable inner = e.getCause();
            assertNotNull(inner);
            assertEquals("foo", inner.getMessage());
        }
    }

    @Test(timeout = 1000)
    public void unwrap_blocks_its_thread_until_the_try_is_completed() throws InterruptedException {
        ExecutorService e = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch unblock = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(1);
            Try<String> t = continuation -> e.submit(() -> {
                unblock.await();
                continuation.ok("foo");
                return null;
            });
            e.submit(() -> {
                start.await();
                assertEquals("foo", t.unwrap());
                done.countDown();
                return null;
            });

            start.countDown();
            unblock.countDown();
            done.await();
        }
        finally {
            e.shutdown();
        }
    }
}

