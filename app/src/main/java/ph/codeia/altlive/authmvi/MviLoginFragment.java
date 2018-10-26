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

import ph.codeia.altlive.Provision;
import ph.codeia.altlive.Receiver;
import ph.codeia.altlive.Task;
import ph.codeia.altlivedata.R;

public class MviLoginFragment extends Fragment {
    private LoginController controller;
    private LoginView view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = ViewModelProviders
                .of(requireActivity(), Provision.variant())
                .get(LoginController.class);
        controller.events().observe(this, Receiver.forSome(event -> event.dispatch(view)));
        controller.state().observe(this, new Task.Progress<Login.Action, Login.Model>() {
            @Override
            public void running(Login.Action action, @Nullable Login.Model currentState) {
                view.indicate(action);
            }

            @Override
            public void done(Login.Action action, Login.Model state) {
                state.render(view);
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
        view = new LoginView((ViewGroup) root, new LoginView.Delegate() {
            @Override
            public void willLogin(
                    @NonNull CharSequence username,
                    @NonNull CharSequence password
            ) {
                controller.login(username, password);
            }

            @Override
            public void willLogout() {
                controller.logout();
            }

            @Override
            public void didLogin(String token) {
            }
        });
        return root;
    }
}

