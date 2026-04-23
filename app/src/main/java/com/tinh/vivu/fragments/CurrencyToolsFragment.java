package com.tinh.vivu.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;

public class CurrencyToolsFragment extends Fragment {

    private TextView tabConverter;
    private TextView tabCalculator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_tools, container, false);

        tabConverter = view.findViewById(R.id.tab_converter);
        tabCalculator = view.findViewById(R.id.tab_calculator);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        tabConverter.setOnClickListener(v -> {
            setActiveTab(tabConverter);
            loadFragment(new CurrencyConverterFragment());
        });

        tabCalculator.setOnClickListener(v -> {
            setActiveTab(tabCalculator);
            loadFragment(new CalculatorFragment());
        });

        setActiveTab(tabConverter);
        loadFragment(new CurrencyConverterFragment());

        return view;
    }

    private void loadFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.currency_content_frame, fragment)
                .commit();
    }

    private void setActiveTab(TextView activeTab) {
        resetTab(tabConverter);
        resetTab(tabCalculator);

        activeTab.setBackgroundResource(R.drawable.bg_tab_active);
        activeTab.setTextColor(Color.parseColor("#FF9100"));
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetTab(TextView tab) {
        tab.setBackgroundResource(0);
        tab.setTextColor(Color.parseColor("#757575"));
        tab.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
}
