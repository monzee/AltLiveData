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

public class LoginController extends ViewModel {
    public static final String MESSAGE_REQUIRED = "This cannot be empty";
    public static final String MESSAGE_BAD_EMAIL = "Invalid email address";

    private final AuthService auth;
    private final LiveTask<Login.Intent, Login.Model> dispatcher;
    private final LiveField<String> toasts;
    private String username;
    private String password;
    private String authToken;
    private Login.Model saved = Login.View::loggedOut;

    public LoginController(AuthService auth, LiveField.Builder builder) {
        this.auth = auth;
        dispatcher = new LiveTask<>(builder, this::dispatch);
        builder.sticky(false);
        toasts = builder.build();
    }

    public Live<String> toasts() {
        return toasts;
    }

    public Live<Task<Login.Intent, Login.Model>> dispatcher() {
        return dispatcher;
    }

    public void login(
            @NonNull CharSequence username,
            @NonNull CharSequence password
    ) {
        this.username = username.toString();
        this.password = password.toString();
        dispatcher.postValue(Login.Intent.LOGIN);
    }

    public void logout() {
        dispatcher.postValue(Login.Intent.LOGOUT);
    }

    public void tell(String text, Object... fmtArgs) {
        toasts.postValue(String.format(text, fmtArgs));
    }

    public void mark(Login.Model model) {
        if (model != null) {
            saved = model;
        }
    }

    public void rollback() {
        dispatcher.postValue(Login.Intent.PUSH);
    }

    private Try<Login.Model> dispatch(Login.Intent intent) {
        switch (intent) {
            case LOGIN:
                String unError = validateUsername(username);
                String pwError = validatePassword(password);
                if (unError != null || pwError != null) {
                    return Try.just(view -> view.invalid(unError, pwError));
                }
                else {
                    return auth.login(username, password).map(token -> {
                        authToken = token;
                        return view -> view.loggedIn(token);
                    });
                }
            case LOGOUT:
                if (authToken == null) {
                    return Try.raise(new IllegalStateException("not logged in"));
                }
                else {
                    return auth.logout(authToken).map(o -> Login.View::loggedOut);
                }
            case PUSH:
                return Try.just(saved);
        }
        return Try.raise(new UnsupportedOperationException(intent.toString()));
    }

    private static String validateUsername(String username) {
        if (username.isEmpty()) {
            return MESSAGE_REQUIRED;
        }
        if (!PatternsCompat.EMAIL_ADDRESS.matcher(username).matches()) {
            return MESSAGE_BAD_EMAIL;
        }
        return null;
    }

    private static String validatePassword(String password) {
        if (password.isEmpty()) {
            return MESSAGE_REQUIRED;
        }
        return null;
    }
}

