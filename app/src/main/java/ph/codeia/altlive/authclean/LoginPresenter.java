package ph.codeia.altlive.authclean;

/*
 * This file is a part of the AltLiveData project.
 */

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Live;
import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.LiveTask;
import ph.codeia.altlive.Task;
import ph.codeia.altlive.Try;
import ph.codeia.altlive.authmvi.Login;

public class LoginPresenter extends ViewModel {
    private final LiveTask<Login.Action, Login.Model> state;
    private final LiveField<Login.Event> events;
    private final Delegate delegate = new Delegate() {
        @Override
        public void didLogin(String token) {
            authToken = token;
        }

        @Override
        public void didLogout() {
            authToken = null;
        }

        @Override
        public void trigger(Login.Event event) {
            events.postValue(event);
        }
    };
    private UseCase useCase;
    private String authToken;

    public LoginPresenter(AuthService auth, LiveField.Builder builder) {
        state = new LiveTask<>(builder, o -> useCase.exec(delegate, auth));
        events = builder.copy().sticky(false).build();
    }

    public Live<Task<Login.Action, Login.Model>> state() {
        return state;
    }

    public Live<Login.Event> events() {
        return events;
    }

    public void login(@NonNull CharSequence username, @NonNull CharSequence password) {
        useCase = new DoLogin(username.toString(), password.toString());
        state.postValue(Login.Action.LOGIN);
    }

    public void logout() {
        useCase = new DoLogout(authToken);
        state.postValue(Login.Action.LOGOUT);
    }

    interface UseCase {
        Try<Login.Model> exec(Delegate delegate, AuthService auth);
    }

    interface Delegate {
        void didLogin(String token);
        void didLogout();
        void trigger(Login.Event event);

        default void tell(String message, Object... fmtArgs) {
            String s = String.format(message, fmtArgs);
            trigger(on -> on.show(s));
        }
    }
}

