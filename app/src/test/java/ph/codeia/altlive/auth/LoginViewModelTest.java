package ph.codeia.altlive.auth;

/*
 * This file is a part of the AltLiveData project.
 */


import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.Loader;
import ph.codeia.altlive.SimulatedAuth;
import ph.codeia.altlive.Task;
import ph.codeia.altlive.android.Threading;

import static org.junit.Assert.*;

public class LoginViewModelTest {
    private final AuthService auth = new SimulatedAuth(Threading.ANYWHERE, 0, 0);
    private final LiveField.Builder builder = new LiveField.Builder();

    @Test
    public void can_emit_toasts() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<String> msg = new AtomicReference<>();
        model.toasts().observeForever(msg::set);

        model.tell("hey %d %s", 1, "foo");
        assertEquals("hey 1 foo", msg.get());
    }

    @Test
    public void validates_credentials_before_logging_in() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<String> e1 = new AtomicReference<>();
        AtomicReference<String> e2 = new AtomicReference<>();
        model.usernameErrors().observeForever(e1::set);
        model.passwordErrors().observeForever(e2::set);

        model.login("", "");
        assertEquals(LoginViewModel.MESSAGE_EMPTY, e1.get());
        assertEquals(LoginViewModel.MESSAGE_EMPTY, e2.get());

        model.login("x", "abcde");
        assertEquals(LoginViewModel.MESSAGE_BAD_EMAIL, e1.get());
        assertNull(e2.get());

        model.login("a@b.c", "abcde");
        assertNull(e1.get());
        assertNull(e2.get());
    }

    @Test
    public void rejects_invalid_emails() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<String> e = new AtomicReference<>();
        model.usernameErrors().observeForever(e::set);

        model.login("foo@example.com", "hunter2");
        assertNull(e.get());
        String[] badEmails = {"a", "@a.com", "a@", "a@b", "a@.com"};
        for (String s : badEmails) {
            model.login(s, "hunter2");
            assertEquals(LoginViewModel.MESSAGE_BAD_EMAIL, e.get());
        }
    }

    @Test
    public void emits_auth_token_when_login_is_successful() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<String> token = new AtomicReference<>();
        model.onLogin().observeForever(Loader.whenDone(token::set));

        model.login("foo@example.com", "hello");
        assertNotNull(token.get());
        assertNotEquals(0, token.get().length());
    }

    @Test(expected = AuthService.Unavailable.class)
    public void emits_error_when_auth_service_is_unavailable() {
        AuthService zeroUptime = new SimulatedAuth(Threading.ANYWHERE, 0, 1);
        LoginViewModel model = new LoginViewModel(zeroUptime, builder);
        AtomicReference<Loader> result = new AtomicReference<>();
        model.onLogin().observeForever(result::set);

        model.login("foo@example.com", "hello");
        assertNotNull(result.get());
        result.get().toTry().unwrap();
    }

    @Test(expected = AuthService.Rejected.class)
    public void emits_error_when_user_is_unknown() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<Loader> result = new AtomicReference<>();
        model.onLogin().observeForever(result::set);

        model.login("a@b.c", "something");
        assertNotNull(result.get());
        result.get().toTry().unwrap();
    }

    @Test(expected = AuthService.Rejected.class)
    public void emits_error_when_password_is_wrong() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<Loader> result = new AtomicReference<>();
        model.onLogin().observeForever(result::set);

        model.login("foo@example.com", "wrong password");
        assertNotNull(result.get());
        result.get().toTry().unwrap();
    }

    @Test
    public void can_clear_login_state() {
        LoginViewModel model = new LoginViewModel(auth, builder);
        AtomicReference<Loader> result = new AtomicReference<>();
        model.onLogin().observeForever(result::set);

        model.login("foo@example.com", "hello");
        assertNotNull(result.get());
        model.clearLoginState();
        assertNull(result.get());
    }
}

