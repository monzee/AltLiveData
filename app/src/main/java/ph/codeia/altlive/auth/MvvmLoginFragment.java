package ph.codeia.altlive.auth;

/*
 * This file is a part of the AltLiveData project.
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.TransitionManager;
import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Loader;
import ph.codeia.altlive.Provision;
import ph.codeia.altlivedata.R;

public class MvvmLoginFragment extends Fragment {
    private LoginViewModel vm;
    private ViewGroup root;
    private TextInputLayout username;
    private TextInputLayout password;
    private Button login;
    private Button logout;
    private View progress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = ViewModelProviders.of(requireActivity(), Provision.DEV)
                .get(LoginViewModel.class);
        vm.usernameErrors().observe(this, error -> showError(username, error));
        vm.passwordErrors().observe(this, error -> showError(password, error));
        vm.toasts().observe(this, this::show);
        vm.onLogin().observe(this, new Loader.Progress<String>() {
            @Override
            public void running(@Nullable String currentValue) {
                indicateProgress(true);
                login.setEnabled(false);
            }

            @Override
            public void done(String value) {
                indicateProgress(false);
                login.setEnabled(false);
                logout.setEnabled(true);
                vm.tell("token: %s", value);
            }

            @Override
            public void failed(Throwable error) {
                try {
                    indicateProgress(false);
                    login.setEnabled(true);
                    vm.clearLoginState();
                    throw error;
                }
                catch (AuthService.Rejected | AuthService.Unavailable e) {
                    vm.tell(e.getMessage());
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
        vm.onLogout().observe(this, new Loader.Progress<Void>() {
            @Override
            public void running(@Nullable Void currentValue) {
                indicateProgress(true);
                logout.setEnabled(false);
            }

            @Override
            public void done(Void value) {
                indicateProgress(false);
                login.setEnabled(true);
                logout.setEnabled(false);
            }

            @Override
            public void failed(Throwable error) {
                try {
                    indicateProgress(false);
                    logout.setEnabled(true);
                    throw error;
                }
                catch (AuthService.Unavailable e) {
                    vm.tell(e.getMessage());
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.login, container, false);
        username = root.findViewById(R.id.username);
        password = root.findViewById(R.id.password);
        login = root.findViewById(R.id.login);
        logout = root.findViewById(R.id.logout);
        progress = root.findViewById(R.id.progress);
        EditText un = username.getEditText();
        EditText pw = password.getEditText();
        assert un != null;
        assert pw != null;
        login.setOnClickListener(o -> vm.login(un.getText(), pw.getText()));
        logout.setOnClickListener(o -> vm.logout());
        un.setOnEditorActionListener((textView, i, keyEvent) -> {
            password.requestFocus();
            return true;
        });
        pw.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (login.isEnabled()) {
                login.performClick();
            }
            return false;
        });
        this.root = (ViewGroup) root;
        return root;
    }

    private void indicateProgress(boolean isBusy) {
        TransitionManager.beginDelayedTransition(root);
        progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }

    private void show(String text) {
        if (text != null) {
            Snackbar.make(root, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private static void showError(TextInputLayout field, String error) {
        field.setError(error);
        field.requestFocus();
    }
}

