package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.TransitionManager;
import ph.codeia.altlivedata.R;

class LoginView implements Login.View {

    interface Delegate {
        void willLogin(
                @NonNull CharSequence username,
                @NonNull CharSequence password
        );
        void willLogout();
        void didLogin(String token);
    }

    private final ViewGroup root;
    private final TextInputLayout username;
    private final TextInputLayout password;
    private final Button login;
    private final Button logout;
    private final View progress;
    private final Delegate delegate;

    LoginView(ViewGroup root, Delegate delegate) {
        Button login = root.findViewById(R.id.login);
        this.root = root;
        this.delegate = delegate;
        this.login = login;
        username = root.findViewById(R.id.username);
        password = root.findViewById(R.id.password);
        logout = root.findViewById(R.id.logout);
        progress = root.findViewById(R.id.progress);
        EditText un = username.getEditText();
        EditText pw = password.getEditText();
        assert un != null && pw != null;
        un.setOnEditorActionListener((textView, i, keyEvent) -> {
            pw.requestFocus();
            return true;
        });
        pw.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (login.isEnabled()) {
                login.performClick();
            }
            return false;
        });
        login.setOnClickListener(o -> delegate.willLogin(un.getText(), pw.getText()));
        logout.setOnClickListener(o -> delegate.willLogout());
    }

    @Override
    public void indicate(Login.Action action) {
        indicateProgress(true);
        switch (action) {
            case LOGIN:
                username.setError(null);
                password.setError(null);
                login.setEnabled(false);
                break;
            case LOGOUT:
                logout.setEnabled(false);
                break;
        }
    }

    @Override
    public void loggedIn(String authToken) {
        indicateProgress(false);
        login.setEnabled(false);
        logout.setEnabled(true);
        delegate.didLogin(authToken);
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
    }

    @Override
    public void show(String message) {
        if (message != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void focusUsername() {
        username.requestFocus();
    }

    @Override
    public void focusPassword() {
        password.requestFocus();
    }

    private void indicateProgress(boolean isBusy) {
        TransitionManager.beginDelayedTransition(root);
        progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }
}

