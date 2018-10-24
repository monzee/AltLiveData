package ph.codeia.altlive.authclean;

/*
 * This file is a part of the AltLiveData project.
 */

import ph.codeia.altlive.AuthService;
import ph.codeia.altlive.Try;
import ph.codeia.altlive.authmvi.Login;
import ph.codeia.altlive.transform.Recover;

class DoLogout implements LoginPresenter.UseCase {
    private static final String MESSAGE_SERVICE_ERROR = "Unable to logout";
    private final String token;

    DoLogout(String token) {
        this.token = token;
    }

    @Override
    public Try<Login.Model> exec(
            LoginPresenter.Delegate delegate,
            AuthService auth
    ) {
        if (token == null) {
            return Try.raise(new IllegalStateException("Not logged in"));
        }
        return auth.logout(token)
                .<Login.Model>map(o -> {
                    delegate.didLogout();
                    return Login.View::loggedOut;
                })
                .pipe(Recover.from(error -> {
                    try {
                        throw error;
                    }
                    catch (AuthService.Unavailable e) {
                        delegate.tell(MESSAGE_SERVICE_ERROR);
                        return view -> view.loggedIn(token);
                    }
                }));
    }
}

