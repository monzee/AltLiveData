package ph.codeia.altlive.transform;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.altlive.Try;

import static org.junit.Assert.*;

/*
 * This file is a part of the AltLiveData project.
 */

public class CircuitBreakerTest {
    @Test
    public void circuit_opens_when_the_source_fails_a_specified_number_of_times() {
        int threshold = 3;
        AtomicInteger counter = new AtomicInteger(0);
        Try<?> e = Try.raise(new ArithmeticException())
                .pipe(Recover.from(t -> {
                    counter.incrementAndGet();
                    throw t;
                }))
                .pipe(CircuitBreaker.of(threshold, 1000L))
                .pipe(Retry.forever().guard(t -> t instanceof ArithmeticException));
        try {
            e.unwrap();
            fail("unreachable");
        }
        catch (CircuitBreaker.Tripped t) {
            assertEquals(threshold, counter.get());
        }
    }
}