package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import java.util.Random;
import java.util.concurrent.Executor;

import ph.codeia.altlive.transform.Execute;

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
        return Execute.on(worker, () -> {
            Thread.sleep(delay);
            if (failFactor < 1 || RNG.nextInt(failFactor) != 0) {
                String credentials = username + ":" + password;
                for (String row : USERS) if (row.equals(credentials)) {
                    return "this-is-your-auth-token";
                }
                throw new Rejected();
            }
            else {
                throw new Unavailable();
            }
        });
    }

    @Override
    public Try<Void> logout(String token) {
        return Execute.on(worker, () -> {
            Thread.sleep(delay);
            if (failFactor < 1 || RNG.nextInt(failFactor) != 0) {
                return null;
            }
            else {
                throw new Unavailable();
            }
        });
    }
}
