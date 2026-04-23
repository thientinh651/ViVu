package com.tinh.vivu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);


        //view.findViewById(R.id.btn_menu_diary).setOnClickListener(v -> showToast("Travel Diary"));

        view.findViewById(R.id.btn_menu_diary).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DiaryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btn_menu_maintenance).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MaintenanceFragment())
                    .addToBackStack(null)
                    .commit();
        });
//        view.findViewById(R.id.btn_menu_rest).setOnClickListener(v -> showToast("Rest Timer"));
//        view.findViewById(R.id.btn_menu_rest).setOnClickListener(v -> {
//            getParentFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new RestTimerFragment())
//                    .addToBackStack(null)
//                    .commit();
//        });
        view.findViewById(R.id.btn_menu_rest).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ClockHostFragment())
                    .addToBackStack(null)
                    .commit();
        });
        view.findViewById(R.id.btn_menu_currency).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CurrencyToolsFragment())
                    .addToBackStack(null)
                    .commit();
        });
        view.findViewById(R.id.btn_menu_settings).setOnClickListener(v -> showToast(R.string.more_settings));

        return view;
    }

    private void showToast(@StringRes int moduleStringId) {
        String module = getString(moduleStringId);
        Toast.makeText(requireContext(), getString(R.string.common_opening_module, module), Toast.LENGTH_SHORT).show();
    }
}
