package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.Random;
import java.util.concurrent.Executor;

public class SimulatedAuth implements AuthService {
    private static final Random RNG = new Random();
    private static final String[] USERS = {
            "foo@example.com:hello",
            "bar@example.com:world",
    };

    private final Executor worker;
    private final long delay;
    private final int failFactor;

    public SimulatedAuth(Executor worker, long delay, int failFactor) {
        this.worker = worker;
        this.delay = delay;
        this.failFactor = failFactor;
    }

    public SimulatedAuth(Executor worker) {
        this(worker, 2500, 10);
    }

    @Override
    public Try<String> login(String username, String password) {
        return continuation -> worker.execute(() -> {
            try {
                Thread.sleep(delay);
                if (failFactor < 1 || RNG.nextInt(failFactor) != 0) {
                    String prefix = username + ":";
                    for (String pair : USERS) if (pair.startsWith(prefix)) {
                        String suffix = pair.substring(prefix.length());
                        if (suffix.equals(password)) {
                            continuation.ok("this-is-your-auth-token");
                            return;
                        }
                    }
                    continuation.error(new Rejected());
                }
                else {
                    continuation.error(new Unavailable());
                }
            }
            catch (InterruptedException e) {
                continuation.error(e);
            }
        });
    }

    @Override
    public Try<Void> logout(String token) {
        return continuation -> worker.execute(() -> {
            try {
                Thread.sleep(delay);
                if (failFactor < 1 || RNG.nextInt(failFactor) != 0) {
                    continuation.ok(null);
                }
                else {
                    continuation.error(new Unavailable());
                }
            }
            catch (InterruptedException e) {
                continuation.error(e);
            }
        });
    }
}
