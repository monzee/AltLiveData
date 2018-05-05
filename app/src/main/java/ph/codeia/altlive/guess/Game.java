package ph.codeia.altlive.guess;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModel;

import ph.codeia.altlive.Live;
import ph.codeia.altlive.LiveTask;
import ph.codeia.altlive.Task;
import ph.codeia.altlive.Try;

class Game extends ViewModel {
    static class State {
    }

    interface Action {
        Try<State> apply(State current);
    }

    private final LiveTask<Action, State> machine = new LiveTask<>(this::step);
    private State current = new State();

    private Try<State> step(Action action) {
        return action.apply(current).map(state -> {
            current = state;
            return state;
        });
    }

    public Live<Task<Action, State>> machine() {
        return machine;
    }

    public void newGame(int maxTries) {
        machine.postValue(_e -> Try.just(new State()));
    }

    public void guess(int n) {
        // TODO
        machine.postValue(Try::just);
    }
}

