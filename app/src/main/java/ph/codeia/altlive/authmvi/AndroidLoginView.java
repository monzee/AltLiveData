package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

class AndroidLoginView implements Login.View {
    final TextInputLayout username;
    final TextInputLayout password;
    final Button login;
    final Button logout;
    private final View progress;
    private final ViewGroup root;

    AndroidLoginView(
            TextInputLayout username,
            TextInputLayout password,
            Button login,
            Button logout,
            View progress,
            ViewGroup root
    ) {
        this.username = username;
        this.password = password;
        this.login = login;
        this.logout = logout;
        this.progress = progress;
        this.root = root;
    }

    @Override
    public void indicate(Login.Intent intent) {
        switch (intent) {
            case LOGIN:
                indicateProgress(true);
                username.setError(null);
                password.setError(null);
                login.setEnabled(false);
                break;
            case LOGOUT:
                indicateProgress(true);
                logout.setEnabled(false);
                break;
        }
    }

    @Override
    public void loggedIn(String authToken) {
        indicateProgress(false);
        login.setEnabled(false);
        logout.setEnabled(true);
        show("token: " + authToken);
    }

    @Override
    public void loggedOut() {
        indicateProgress(false);
        logout.setEnabled(false);
        login.setEnabled(true);
    }

    @Override
    public void invalid(
            @Nullable String usernameError,
            @Nullable String passwordError
    ) {
        indicateProgress(false);
        login.setEnabled(true);
        username.setError(usernameError);
        password.setError(passwordError);
        if (usernameError != null) {
            username.requestFocus();
        }
        else if (passwordError != null) {
            password.requestFocus();
        }
    }

    @Override
    public void show(String message) {
        if (message != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void indicateProgress(boolean isBusy) {
        TransitionManager.beginDelayedTransition(root);
        progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }
}

