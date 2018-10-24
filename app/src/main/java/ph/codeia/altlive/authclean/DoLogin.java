package ph.codeia.altlive.authclean;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.v4.util.PatternsCompat;

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Try;
import ph.codeia.altlive.authmvi.Login;
import ph.codeia.altlive.transform.Recover;

class DoLogin implements LoginPresenter.UseCase {
    private static final String MESSAGE_REQUIRED = "This cannot be empty";
    private static final String MESSAGE_BAD_EMAIL = "Malformed email";
    private static final String MESSAGE_LOGGED_IN = "Login successful";
    private static final String MESSAGE_SERVICE_ERROR = "Can't login: %s";
    private final String username;
    private final String password;

    DoLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Try<Login.Model> exec(
            LoginPresenter.Delegate delegate,
            AuthService auth
    ) {
        Errors errors = validate(username, password);
        if (errors.present()) {
            delegate.trigger(errors);
            return Try.just(errors);
        }
        return auth.login(username, password)
                .<Login.Model>map(token -> {
                    delegate.tell(MESSAGE_LOGGED_IN);
                    delegate.didLogin(token);
                    return view -> view.loggedIn(token);
                })
                .pipe(Recover.from(error -> {
                    try {
                        throw error;
                    }
                    catch (AuthService.Unavailable | AuthService.Rejected e) {
                        delegate.tell(MESSAGE_SERVICE_ERROR, e.getMessage());
                        return Login.View::loggedOut;
                    }
                }));
    }

    private static Errors validate(String username, String password) {
        Errors result = new Errors();
        if (password == null || password.isEmpty()) {
            result.password = MESSAGE_REQUIRED;
        }
        if (username == null || username.isEmpty()) {
            result.username = MESSAGE_REQUIRED;
        }
        else if (!PatternsCompat.EMAIL_ADDRESS.matcher(username).matches()) {
            result.username = MESSAGE_BAD_EMAIL;
        }
        return result;
    }

    private static class Errors implements Login.Model, Login.Event {
        String username;
        String password;

        boolean present() {
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
            else if (password != null) {
                on.focusPassword();
            }
        }
    }
}

