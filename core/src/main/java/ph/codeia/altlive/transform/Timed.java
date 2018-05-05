package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

public class Timed<T> implements Try<T> {

    public static class Partial<T> {
        private final long timeoutMillis;

        private Partial(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public Function<Try<T>, Timed<T>> on(Executor executor) {
            return source -> new Timed<>(timeoutMillis, executor, source);
        }
    }

    public static <T> Partial<T> of(long timeout, TimeUnit units) {
        return millis(units.toMillis(timeout));
    }

    public static <T> Partial<T> millis(long timeoutMillis) {
        return new Partial<>(timeoutMillis);
    }

    private final CompletionService<T> completionService;
    private final long timeoutMillis;
    private final Try<T> source;

    public Timed(
            long timeoutMillis,
            CompletionService<T> completionService,
            Try<T> source
    ) {
        this.completionService = completionService;
        this.timeoutMillis = timeoutMillis;
        this.source = source;
    }

    public Timed(long timeoutMillis, Executor executor, Try<T> source) {
        this(timeoutMillis, new ExecutorCompletionService<>(executor), source);
    }

    @Override
    public void select(Case<? super T> continuation) {
        Future<T> result = completionService.submit(source::unwrap);
        try {
            continuation.ok(result.get(timeoutMillis, TimeUnit.MILLISECONDS));
        }
        catch (ExecutionException e) {
            continuation.error(e.getCause());
        }
        catch (InterruptedException | TimeoutException e) {
            continuation.error(e);
        }
    }
}

