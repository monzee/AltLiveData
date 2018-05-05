package ph.codeia.altlive.authmvi;

/*
 * This file is a part of the AltLiveData project.
 */

import android.support.annotation.Nullable;

public interface Login {
    enum Intent {
        LOGIN, LOGOUT, PUSH
    }

    interface Model {
        void render(View view);
    }

    interface View {
        void indicate(Intent intent);
        void loggedIn(String authToken);
        void loggedOut();
        void invalid(@Nullable String usernameError, @Nullable String passwordError);
        void show(String message);
    }
}

