package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.Nullable;

public interface Login {
    enum Action {
        LOGIN, LOGOUT
    }

    interface Model {
        void render(View view);
    }

    interface View extends Effects {
        void indicate(Action action);
        void loggedIn(String authToken);
        void loggedOut();
        void invalid(@Nullable String usernameError, @Nullable String passwordError);
    }

    interface Event {
        void dispatch(Effects on);
    }

    interface Effects {
        void show(String message);
        void focusUsername();
        void focusPassword();
    }
}

