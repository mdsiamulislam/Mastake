package me.binarylife.mastaka;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TodoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        TextView textView = new TextView(requireContext());
        textView.setText("My Todo List");
        textView.setTextSize(22);
        textView.setPadding(32, 32, 32, 32);

        return textView;
    }
}
