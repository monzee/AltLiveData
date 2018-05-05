package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Provision;
import ph.codeia.altlive.Task;
import ph.codeia.altlivedata.R;

public class MviLoginFragment extends Fragment {
    private LoginController controller;
    private Login.View view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = ViewModelProviders.of(requireActivity(), Provision.DEV)
                .get(LoginController.class);
        controller.toasts().observe(this, message -> view.show(message));
        controller.dispatcher().observe(this, new Task.Progress<Login.Intent, Login.Model>() {
            @Override
            public void running(Login.Intent key, @Nullable Login.Model currentValue) {
                controller.mark(currentValue);
                view.indicate(key);
            }

            @Override
            public void done(Login.Intent key, Login.Model value) {
                value.render(view);
            }

            @Override
            public void failed(Login.Intent key, Throwable error) {
                try {
                    controller.rollback();
                    throw error;
                }
                catch (AuthService.Rejected | AuthService.Unavailable e) {
                    controller.tell(e.getMessage());
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
        AndroidLoginView view = new AndroidLoginView(
                root.findViewById(R.id.username),
                root.findViewById(R.id.password),
                root.findViewById(R.id.login),
                root.findViewById(R.id.logout),
                root.findViewById(R.id.progress),
                (ViewGroup) root
        );
        EditText un = view.username.getEditText();
        EditText pw = view.password.getEditText();
        assert un != null;
        assert pw != null;
        un.setOnEditorActionListener((textView, i, keyEvent) -> {
            pw.requestFocus();
            return true;
        });
        pw.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (view.login.isEnabled()) {
                view.login.performClick();
            }
            return false;
        });
        view.login.setOnClickListener(o -> controller.login(un.getText(), pw.getText()));
        view.logout.setOnClickListener(o -> controller.logout());
        this.view = view;
        return root;
    }
}

