package ph.codeia.altlive.cmp;

/*
 * This file is a part of the AltLiveData project.
 */

import android.annotation.SuppressLint;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.android.AndroidLive;
import ph.codeia.altlivedata.R;

@SuppressLint("SetTextI18n")
public class ComparisonActivity extends AppCompatActivity {

    public static class Model extends ViewModel {
        final MutableLiveData<CharSequence> data = new MutableLiveData<>();
        final LiveField<CharSequence> transientData = AndroidLive.builder().sticky(false).build();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livedata);
        Model vm = ViewModelProviders.of(this).get(Model.class);
        EditText input = findViewById(R.id.in_content);
        TextView output = findViewById(R.id.out_content);
        Button set = findViewById(R.id.set_content);
        set.setOnClickListener(view -> {
            vm.data.setValue(input.getText());
            vm.transientData.setValue(input.getText());
        });
        input.setOnEditorActionListener((textView, i, keyEvent) -> {
            set.performClick();
            return false;
        });
        vm.data.observe(this, text -> output.setText("activity MutableLiveData: " + text));
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment, new Inner(), "inner")
                    .commitNow();
        }
    }

    public static class Inner extends Fragment {
        private TextView output;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Model vm = ViewModelProviders.of(requireActivity()).get(Model.class);
            vm.data.observe(this, text -> output.setText("fragment MutableLiveData: " + text));
        }

        @Nullable
        @Override
        public View onCreateView(
                @NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState
        ) {
            View root = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
            output = (TextView) root;
            return root;
        }
    }
}

