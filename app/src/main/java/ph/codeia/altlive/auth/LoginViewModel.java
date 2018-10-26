package ph.codeia.altlive.auth;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.v4.util.PatternsCompat;

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Live;
import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.LiveLoader;
import ph.codeia.altlive.Loader;
import ph.codeia.altlive.Try;

public class LoginViewModel extends ViewModel {
    public static final String MESSAGE_EMPTY = "This cannot be empty";
    public static final String MESSAGE_BAD_EMAIL = "Invalid email";

    private final AuthService auth;
    private final LiveField<String> toasts;
    private final LiveField<String> usernameErrors;
    private final LiveField<String> passwordErrors;
    private final LiveLoader<Void> logout;
    private final LiveLoader<String> login;
    private String authToken;

    public LoginViewModel(AuthService auth, LiveField.Builder builder) {
        this.auth = auth;
        usernameErrors = builder.build();
        passwordErrors = builder.build();
        login = new LiveLoader<>(builder);
        LiveField.Builder stickyBuilder = builder.copy().sticky(false);
        toasts = stickyBuilder.build();
        logout = new LiveLoader<>(stickyBuilder);
    }

    public Live<String> toasts() {
        return toasts;
    }

    public Live<String> usernameErrors() {
        return usernameErrors;
    }

    public Live<String> passwordErrors() {
        return passwordErrors;
    }

    public Live<Loader<String>> onLogin() {
        return login;
    }

    public Live<Loader<Void>> onLogout() {
        return logout;
    }

    public void tell(String text, Object... fmtArgs) {
        toasts.postValue(String.format(text, fmtArgs));
    }

    public void clearLoginState() {
        login.postValue(null);
    }

    public void login(
            @NonNull CharSequence username,
            @NonNull CharSequence password
    ) {
        String un = username.toString();
        String pw = password.toString();
        if (isValid(un, pw)) {
            login.postValue(auth.login(un, pw).map(token -> {
                authToken = token;
                return token;
            }));
        }
    }

    public void logout() {
        if (authToken != null) {
            logout.postValue(auth.logout(authToken));
        }
        else {
            logout.postValue(Try.raise(new IllegalStateException("not logged in")));
        }
    }

    private boolean isValid(String username, String password) {
        boolean isValid = true;
        usernameErrors.postValue(null);
        passwordErrors.postValue(null);
        if (username.isEmpty()) {
            usernameErrors.postValue(MESSAGE_EMPTY);
            isValid = false;
        }
        else if (!isEmail(username)) {
            usernameErrors.postValue(MESSAGE_BAD_EMAIL);
            isValid = false;
        }
        if (password.isEmpty()) {
            passwordErrors.postValue(MESSAGE_EMPTY);
            isValid = false;
        }
        return isValid;
    }

    private static boolean isEmail(String s) {
        return PatternsCompat.EMAIL_ADDRESS.matcher(s).matches();
    }
}

