package ph.codeia.altlive.guess;

/*
 * This file is a part of the AltLiveData project.
 */

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.altlive.Task;

public class GuessingGameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Game game = ViewModelProviders.of(this).get(Game.class);
        game.machine().observe(this, Task.whenDone(state -> {

        }));
    }
}

