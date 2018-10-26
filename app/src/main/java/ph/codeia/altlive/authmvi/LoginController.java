package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.v4.util.PatternsCompat;

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Live;
import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.LiveTask;
import ph.codeia.altlive.Task;
import ph.codeia.altlive.Try;
import ph.codeia.altlive.transform.Recover;

public class LoginController extends ViewModel {
    public static final String MESSAGE_REQUIRED = "This cannot be empty";
    public static final String MESSAGE_BAD_EMAIL = "Invalid email address";

    private final AuthService auth;
    private final LiveTask<Login.Action, Login.Model> state;
    private final LiveField<Login.Event> events;
    private String username;
    private String password;
    private String authToken;

    public LoginController(AuthService auth, LiveField.Builder builder) {
        this.auth = auth;
        state = new LiveTask<>(builder, this::dispatch);
        events = builder.copy().sticky(false).build();
    }

    public Live<Task<Login.Action, Login.Model>> state() {
        return state;
    }

    public Live<Login.Event> events() {
        return events;
    }

    public void login(
            @NonNull CharSequence username,
            @NonNull CharSequence password
    ) {
        this.username = username.toString();
        this.password = password.toString();
        state.postValue(Login.Action.LOGIN);
    }

    public void logout() {
        state.postValue(Login.Action.LOGOUT);
    }

    public void tell(String text, Object... fmtArgs) {
        String message = String.format(text, fmtArgs);
        events.postValue(on -> on.show(message));
    }

    private Try<Login.Model> dispatch(Login.Action action) {
        switch (action) {
            case LOGIN:
                Errors errors = validate(username, password);
                if (errors.any()) {
                    events.postValue(errors);
                    return Try.just(errors);
                }
                return auth.login(username, password)
                        .<Login.Model>map(token -> {
                            authToken = token;
                            tell("token: %s", token);
                            return view -> view.loggedIn(token);
                        })
                        .pipe(Recover.from(error -> {
                            try {
                                throw error;
                            }
                            catch (AuthService.Unavailable | AuthService.Rejected e) {
                                tell("Unable to login. %s.", e.getMessage());
                                return Login.View::loggedOut;
                            }
                        }));
            case LOGOUT:
                if (authToken == null) {
                    return Try.raise(new IllegalStateException("not logged in"));
                }
                return auth.logout(authToken)
                        .<Login.Model>map(o -> Login.View::loggedOut)
                        .pipe(Recover.from(error -> {
                            try {
                                throw error;
                            }
                            catch (AuthService.Unavailable e) {
                                tell("Unable to logout. %s.", e.getMessage());
                                return view -> view.loggedIn(authToken);
                            }
                        }));
        }
        return Try.raise(new UnsupportedOperationException(action.toString()));
    }

    private static Errors validate(String username, String password) {
        Errors result = new Errors();
        if (username.isEmpty()) {
            result.username = MESSAGE_REQUIRED;
        }
        else if (!PatternsCompat.EMAIL_ADDRESS.matcher(username).matches()) {
            result.username = MESSAGE_BAD_EMAIL;
        }
        if (password.isEmpty()) {
            result.password = MESSAGE_REQUIRED;
        }
        return result;
    }

    private static class Errors implements Login.Model, Login.Event {
        String username;
        String password;

        boolean any() {
            return username != null || password != null;
        }

        @Override
        public void render(Login.View view) {
            view.invalid(username, password);
        }

        @Override
        public void dispatch(Login.Effects on) {
            if (username != null) {
                on.focusUsername();
            }
            else {
                on.focusPassword();
            }
        }
    }
}

