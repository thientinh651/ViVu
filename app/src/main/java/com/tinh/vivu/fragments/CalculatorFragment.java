package com.tinh.vivu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tinh.vivu.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CalculatorFragment extends Fragment {

    private final DecimalFormat calculatorFormatter = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.US));

    private TextView tvExpression;
    private TextView tvResult;
    private String currentInput = "0";
    private Double storedValue = null;
    private String pendingOperator = null;
    private boolean resetInputOnNextDigit = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);

        tvExpression = view.findViewById(R.id.tv_calculator_expression);
        tvResult = view.findViewById(R.id.tv_calculator_result);

        setupCalculator(view);
        updateCalculatorDisplay();

        return view;
    }

    private void setupCalculator(View view) {
        View.OnClickListener digitListener = v -> appendDigit(((Button) v).getText().toString());
        int[] digitButtonIds = {
                R.id.btn_calc_0, R.id.btn_calc_1, R.id.btn_calc_2, R.id.btn_calc_3, R.id.btn_calc_4,
                R.id.btn_calc_5, R.id.btn_calc_6, R.id.btn_calc_7, R.id.btn_calc_8, R.id.btn_calc_9
        };
        for (int buttonId : digitButtonIds) {
            view.findViewById(buttonId).setOnClickListener(digitListener);
        }

        view.findViewById(R.id.btn_calc_decimal).setOnClickListener(v -> appendDecimalPoint());
        view.findViewById(R.id.btn_calc_clear).setOnClickListener(v -> clearCalculator());
        view.findViewById(R.id.btn_calc_delete).setOnClickListener(v -> deleteLastCharacter());
        view.findViewById(R.id.btn_calc_percent).setOnClickListener(v -> applyPercent());
        view.findViewById(R.id.btn_calc_sign).setOnClickListener(v -> toggleSign());
        view.findViewById(R.id.btn_calc_plus).setOnClickListener(v -> performOperator("+"));
        view.findViewById(R.id.btn_calc_minus).setOnClickListener(v -> performOperator("-"));
        view.findViewById(R.id.btn_calc_multiply).setOnClickListener(v -> performOperator("*"));
        view.findViewById(R.id.btn_calc_divide).setOnClickListener(v -> performOperator("/"));
        view.findViewById(R.id.btn_calc_equals).setOnClickListener(v -> calculateResult());
    }

    private void appendDigit(String digit) {
        if (resetInputOnNextDigit) {
            currentInput = "0";
            resetInputOnNextDigit = false;
        }

        if ("0".equals(currentInput)) {
            currentInput = digit;
        } else {
            currentInput += digit;
        }

        updateCalculatorDisplay();
    }

    private void appendDecimalPoint() {
        if (resetInputOnNextDigit) {
            currentInput = "0";
            resetInputOnNextDigit = false;
        }

        if (!currentInput.contains(".")) {
            currentInput += ".";
            updateCalculatorDisplay();
        }
    }

    private void deleteLastCharacter() {
        if (resetInputOnNextDigit) {
            currentInput = "0";
            resetInputOnNextDigit = false;
            updateCalculatorDisplay();
            return;
        }

        if (currentInput.length() <= 1) {
            currentInput = "0";
        } else {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if ("-".equals(currentInput) || currentInput.isEmpty()) {
                currentInput = "0";
            }
        }

        updateCalculatorDisplay();
    }

    private void applyPercent() {
        double value = parseDouble(currentInput);
        currentInput = formatCalculatorValue(value / 100d);
        resetInputOnNextDigit = false;
        updateCalculatorDisplay();
    }

    private void toggleSign() {
        double value = parseDouble(currentInput);
        currentInput = formatCalculatorValue(value * -1d);
        resetInputOnNextDigit = false;
        updateCalculatorDisplay();
    }

    private void performOperator(String operator) {
        double currentValue = parseDouble(currentInput);

        if (storedValue == null) {
            storedValue = currentValue;
        } else if (pendingOperator != null && !resetInputOnNextDigit) {
            Double result = applyOperation(storedValue, currentValue, pendingOperator);
            if (result == null) {
                return;
            }
            storedValue = result;
            currentInput = formatCalculatorValue(result);
        }

        pendingOperator = operator;
        resetInputOnNextDigit = true;
        updateCalculatorDisplay();
    }

    private void calculateResult() {
        if (storedValue == null || pendingOperator == null) {
            updateCalculatorDisplay();
            return;
        }

        Double result = applyOperation(storedValue, parseDouble(currentInput), pendingOperator);
        if (result == null) {
            return;
        }

        currentInput = formatCalculatorValue(result);
        storedValue = null;
        pendingOperator = null;
        resetInputOnNextDigit = true;
        updateCalculatorDisplay();
    }

    private Double applyOperation(double firstValue, double secondValue, String operator) {
        switch (operator) {
            case "+":
                return firstValue + secondValue;
            case "-":
                return firstValue - secondValue;
            case "*":
                return firstValue * secondValue;
            case "/":
                if (secondValue == 0) {
                    Toast.makeText(requireContext(), R.string.calculator_error_divide_by_zero, Toast.LENGTH_SHORT).show();
                    clearCalculator();
                    return null;
                }
                return firstValue / secondValue;
            default:
                return secondValue;
        }
    }

    private void clearCalculator() {
        currentInput = "0";
        storedValue = null;
        pendingOperator = null;
        resetInputOnNextDigit = false;
        updateCalculatorDisplay();
    }

    private void updateCalculatorDisplay() {
        tvResult.setText(currentInput);

        if (storedValue != null && pendingOperator != null) {
            if (resetInputOnNextDigit) {
                tvExpression.setText(formatCalculatorValue(storedValue) + getOperatorLabel(pendingOperator));
            } else {
                tvExpression.setText(formatCalculatorValue(storedValue) + getOperatorLabel(pendingOperator) + currentInput);
            }
        } else {
            tvExpression.setText("");
        }
    }

    private String getOperatorLabel(String operator) {
        switch (operator) {
            case "*":
                return "x";
            case "/":
                return "÷";
            default:
                return operator;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String formatCalculatorValue(double value) {
        return calculatorFormatter.format(value);
    }
}
