package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import ph.codeia.altlive.android.AndroidLive;
import ph.codeia.altlive.android.Threading;
import ph.codeia.altlive.auth.LoginViewModel;
import ph.codeia.altlive.authmvi.LoginController;
import ph.codeia.altlivedata.BuildConfig;

public enum Provision implements ViewModelProvider.Factory {
    DEV {
        @Override
        protected AuthService authService() {
            return Dev.AUTH_SERVICE;
        }
    },
    PRODUCTION;

    public static Provision variant() {
        return BuildConfig.DEBUG ? DEV : PRODUCTION;
    }

    private static class Production {
        static final LiveField.Builder LIVE_BUILDER = AndroidLive.builder(false);
    }

    private static class Dev {
        static final AuthService AUTH_SERVICE = new SimulatedAuth(Threading.CPU_BOUND, 2500, 2);
    }

    protected LiveField.Builder liveBuilder() {
        return Production.LIVE_BUILDER;
    }

    protected AuthService authService() {
        throw new RuntimeException("TODO: replace with the real thing");
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return modelClass.cast(new LoginViewModel(authService(), liveBuilder()));
        }
        if (modelClass.isAssignableFrom(LoginController.class)) {
            return modelClass.cast(new LoginController(authService(), liveBuilder()));
        }
        throw new IllegalArgumentException("Don't know how to make " + modelClass.getSimpleName());
    }
}
