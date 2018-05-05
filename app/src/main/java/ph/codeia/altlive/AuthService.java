package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

public interface AuthService {
    Try<String> login(String username, String password);
    Try<Void> logout(String token);

    class Rejected extends RuntimeException {
        Rejected() {
            super("Invalid credentials");
        }
    }

    class Unavailable extends RuntimeException {
        Unavailable() {
            super("Login service unavailable");
        }
    }
}
