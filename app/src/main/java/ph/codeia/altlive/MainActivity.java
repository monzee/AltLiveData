package ph.codeia.altlive;

/*
 * This file is a part of the AltLiveData project.
 */

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import ph.codeia.altlive.authmvi.MviLoginFragment;
import ph.codeia.altlivedata.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chrome);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.root, new MviLoginFragment())
                    .commit();
        }
    }
}

