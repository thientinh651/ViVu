package com.tinh.vivu.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;
import com.tinh.vivu.utils.ValidationUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CurrencyConverterFragment extends Fragment {
    private static final double MAX_AMOUNT = 1_000_000_000_000d;

    private static final String[] CURRENCY_CODES = {"USD", "VND", "EUR", "GBP", "JPY", "THB", "SGD"};
    private static final double[] QUICK_VALUES = {1, 5, 10, 20, 50, 100};

    private final Map<String, Double> exchangeRates = new HashMap<>();
    private final Map<String, String> currencyNames = new HashMap<>();
    private final Map<String, String> currencySymbols = new HashMap<>();

    private final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private final DecimalFormat rateFormatter = new DecimalFormat("#,##0.####", DecimalFormatSymbols.getInstance(Locale.US));

    private EditText etAmount;
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    private View btnSwapCurrency;
    private TextView tvFromName;
    private TextView tvToName;
    private TextView tvConvertedAmount;
    private TextView tvExchangeRate;
    private TextView[] quickFromViews;
    private TextView[] quickToViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_converter, container, false);

        initializeCurrencyData();
        bindViews(view);
        setupConverter();

        return view;
    }

    private void initializeCurrencyData() {
        exchangeRates.clear();
        currencyNames.clear();
        currencySymbols.clear();

        exchangeRates.put("USD", 1.0);
        exchangeRates.put("VND", 26325.0);
        exchangeRates.put("EUR", 0.85);
        exchangeRates.put("GBP", 0.74);
        exchangeRates.put("JPY", 159.55);
        exchangeRates.put("THB", 32.14);
        exchangeRates.put("SGD", 1.27);

        currencyNames.put("USD", getString(R.string.currency_name_usd));
        currencyNames.put("VND", getString(R.string.currency_name_vnd));
        currencyNames.put("EUR", getString(R.string.currency_name_eur));
        currencyNames.put("GBP", getString(R.string.currency_name_gbp));
        currencyNames.put("JPY", getString(R.string.currency_name_jpy));
        currencyNames.put("THB", getString(R.string.currency_name_thb));
        currencyNames.put("SGD", getString(R.string.currency_name_sgd));

        currencySymbols.put("USD", "$");
        currencySymbols.put("VND", "VND ");
        currencySymbols.put("EUR", "EUR ");
        currencySymbols.put("GBP", "GBP ");
        currencySymbols.put("JPY", "JPY ");
        currencySymbols.put("THB", "THB ");
        currencySymbols.put("SGD", "SGD ");
    }

    private void bindViews(View view) {
        etAmount = view.findViewById(R.id.et_amount);
        spinnerFrom = view.findViewById(R.id.spinner_from);
        spinnerTo = view.findViewById(R.id.spinner_to);
        btnSwapCurrency = view.findViewById(R.id.btn_swap_currency);
        tvFromName = view.findViewById(R.id.tv_from_name);
        tvToName = view.findViewById(R.id.tv_to_name);
        tvConvertedAmount = view.findViewById(R.id.tv_converted_amount);
        tvExchangeRate = view.findViewById(R.id.tv_exchange_rate);

        quickFromViews = new TextView[]{
                view.findViewById(R.id.tv_quick_from_1),
                view.findViewById(R.id.tv_quick_from_2),
                view.findViewById(R.id.tv_quick_from_3),
                view.findViewById(R.id.tv_quick_from_4),
                view.findViewById(R.id.tv_quick_from_5),
                view.findViewById(R.id.tv_quick_from_6)
        };
        quickToViews = new TextView[]{
                view.findViewById(R.id.tv_quick_to_1),
                view.findViewById(R.id.tv_quick_to_2),
                view.findViewById(R.id.tv_quick_to_3),
                view.findViewById(R.id.tv_quick_to_4),
                view.findViewById(R.id.tv_quick_to_5),
                view.findViewById(R.id.tv_quick_to_6)
        };
    }

    private void setupConverter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, CURRENCY_CODES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        spinnerFrom.setSelection(0);
        spinnerTo.setSelection(1);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateConverter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateConverter();
            }
        };

        spinnerFrom.setOnItemSelectedListener(itemSelectedListener);
        spinnerTo.setOnItemSelectedListener(itemSelectedListener);

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConverter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnSwapCurrency.setOnClickListener(v -> {
            int fromPosition = spinnerFrom.getSelectedItemPosition();
            int toPosition = spinnerTo.getSelectedItemPosition();
            spinnerFrom.setSelection(toPosition);
            spinnerTo.setSelection(fromPosition);
        });

        updateConverter();
    }

    private void updateConverter() {
        String fromCode = getSelectedCurrencyCode(spinnerFrom, "USD");
        String toCode = getSelectedCurrencyCode(spinnerTo, "VND");
        String rawAmount = etAmount.getText().toString().trim();
        double amount = 0;
        boolean hasInvalidInput = false;

        if (!rawAmount.isEmpty()) {
            if (!ValidationUtils.isValidDecimalFormat(rawAmount, 12, 2) || !ValidationUtils.isNumeric(rawAmount)) {
                etAmount.setError(getString(R.string.currency_invalid_amount_format));
                hasInvalidInput = true;
            } else {
                amount = parseDouble(rawAmount);
                if (!ValidationUtils.isNumberInRange(amount, 0, MAX_AMOUNT)) {
                    etAmount.setError(getString(R.string.currency_invalid_amount_range));
                    hasInvalidInput = true;
                } else {
                    etAmount.setError(null);
                }
            }
        } else {
            etAmount.setError(null);
        }

        double fromRate = exchangeRates.containsKey(fromCode) ? exchangeRates.get(fromCode) : 1.0;
        double toRate = exchangeRates.containsKey(toCode) ? exchangeRates.get(toCode) : 1.0;
        double convertedAmount = hasInvalidInput || fromRate == 0 ? 0 : (amount / fromRate) * toRate;

        tvFromName.setText(currencyNames.get(fromCode));
        tvToName.setText(currencyNames.get(toCode));
        tvConvertedAmount.setText(getCurrencySymbol(toCode) + formatAmount(convertedAmount));
        tvExchangeRate.setText(getString(R.string.currency_exchange_rate_format, fromCode, formatRate(toRate / fromRate), toCode));

        for (int i = 0; i < QUICK_VALUES.length; i++) {
            double quickValue = QUICK_VALUES[i];
            double quickConverted = (quickValue / fromRate) * toRate;
            quickFromViews[i].setText(getCurrencySymbol(fromCode) + formatAmount(quickValue));
            quickToViews[i].setText(getCurrencySymbol(toCode) + formatAmount(quickConverted));
        }
    }

    private String getSelectedCurrencyCode(Spinner spinner, String fallback) {
        Object selectedItem = spinner.getSelectedItem();
        return selectedItem instanceof String ? (String) selectedItem : fallback;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String formatAmount(double value) {
        return amountFormatter.format(value);
    }

    private String formatRate(double value) {
        return rateFormatter.format(value);
    }

    private String getCurrencySymbol(String code) {
        String symbol = currencySymbols.get(code);
        return symbol == null ? "" : symbol;
    }
}
