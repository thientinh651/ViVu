package com.tinh.vivu.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.RouteStopAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripDetailFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private static final String DATETIME_FORMAT = "HH:mm dd/MM/yyyy";
    private static final int MIN_LOCATION_NAME_LENGTH = 2;
    private static final int MAX_LOCATION_NAME_LENGTH = 80;

    private TextView tvTripName;
    private TextView tvDate;
    private ImageView btnBack;
    private RecyclerView rvStops;
    private MaterialButton btnAddStop;
    private TextView tabPlanning;
    private TextView tabOngoing;
    private TextView tabCompleted;

    private AppDatabase database;
    private ExecutorService executorService;
    private RouteStopAdapter adapter;
    private final List<RouteStop> stopList = new ArrayList<>();

    private int currentTripId = -1;
    private Trip currentTrip;

    public static TripDetailFragment newInstance(int tripId) {
        TripDetailFragment fragment = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TRIP_ID, tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentTripId = getArguments().getInt(ARG_TRIP_ID, -1);
        }

        initViews(view);
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        adapter = new RouteStopAdapter();
        rvStops.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStops.setAdapter(adapter);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnAddStop.setOnClickListener(v -> showAddStopDialog());
        tabPlanning.setOnClickListener(v -> updateTripStatus("Planning"));
        tabOngoing.setOnClickListener(v -> updateTripStatus("Ongoing"));
        tabCompleted.setOnClickListener(v -> updateTripStatus("Completed"));

        adapter.setOnStopClickListener(new RouteStopAdapter.OnStopClickListener() {
            @Override
            public void onDeleteClick(RouteStop stop) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.trip_detail_delete_stop_title)
                        .setMessage(R.string.trip_detail_delete_stop_message)
                        .setPositiveButton(R.string.home_yes, (dialog, which) -> executorService.execute(() -> {
                            database.routeStopDao().delete(stop);
                            reorderStops();
                        }))
                        .setNegativeButton(R.string.home_no, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onUpdateClick(RouteStop stop) {
                executorService.execute(() -> database.routeStopDao().update(stop));
            }
        });

        if (currentTripId == -1) {
            Toast.makeText(requireContext(), getString(R.string.trip_detail_toast_trip_not_found), Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        loadTripDetails();
        loadRouteStops();
    }

    private void initViews(View view) {
        tvTripName = view.findViewById(R.id.tv_detail_trip_name);
        tvDate = view.findViewById(R.id.tv_detail_date);
        btnBack = view.findViewById(R.id.btn_back);
        rvStops = view.findViewById(R.id.rv_detail_stops);
        btnAddStop = view.findViewById(R.id.btn_add_route_stop);
        tabPlanning = view.findViewById(R.id.tab_planning);
        tabOngoing = view.findViewById(R.id.tab_ongoing);
        tabCompleted = view.findViewById(R.id.tab_completed);
    }

    private void reorderStops() {
        List<RouteStop> remainingStops = database.routeStopDao().getStopsByTripId(currentTripId);
        for (int i = 0; i < remainingStops.size(); i++) {
            RouteStop stop = remainingStops.get(i);
            stop.setOrderIndex(i + 1);
            database.routeStopDao().update(stop);
        }
        loadRouteStops();
    }

    private void updateTripStatus(String newStatus) {
        if (currentTrip == null) {
            return;
        }

        currentTrip.setStatus(newStatus);
        updateTabUI(newStatus);
        executorService.execute(() -> database.tripDao().update(currentTrip));
    }

    private void updateTabUI(String status) {
        tabPlanning.setBackgroundResource(0);
        tabPlanning.setTextColor(Color.WHITE);
        tabOngoing.setBackgroundResource(0);
        tabOngoing.setTextColor(Color.WHITE);
        tabCompleted.setBackgroundResource(0);
        tabCompleted.setTextColor(Color.WHITE);

        int tealColor = Color.parseColor("#00897B");
        if ("Ongoing".equals(status)) {
            tabOngoing.setBackgroundResource(R.drawable.bg_tab_active);
            tabOngoing.setTextColor(tealColor);
        } else if ("Completed".equals(status)) {
            tabCompleted.setBackgroundResource(R.drawable.bg_tab_active);
            tabCompleted.setTextColor(tealColor);
        } else {
            tabPlanning.setBackgroundResource(R.drawable.bg_tab_active);
            tabPlanning.setTextColor(tealColor);
        }
    }

    private void showAddStopDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_stop);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etName = dialog.findViewById(R.id.et_location_name);
        EditText etArr = dialog.findViewById(R.id.et_expected_arrival);
        EditText etDep = dialog.findViewById(R.id.et_expected_departure);
        MaterialButton btnAdd = dialog.findViewById(R.id.btn_add);

        etArr.setOnClickListener(v -> pickDateTime(etArr));
        etDep.setOnClickListener(v -> pickDateTime(etDep));

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String arr = etArr.getText().toString().trim();
            String dep = etDep.getText().toString().trim();

            clearErrors(etName, etArr, etDep);

            if (ValidationUtils.isNullOrEmpty(name)) {
                showFieldError(etName, getString(R.string.trip_detail_error_location_required));
                return;
            }
            if (!ValidationUtils.isValidDisplayName(name, MIN_LOCATION_NAME_LENGTH, MAX_LOCATION_NAME_LENGTH)) {
                showFieldError(etName, getString(R.string.trip_detail_error_location_invalid));
                return;
            }
            if (ValidationUtils.isNullOrEmpty(arr)) {
                showFieldError(etArr, getString(R.string.trip_detail_error_arrival_required));
                return;
            }
            if (!ValidationUtils.isValidDate(arr, DATETIME_FORMAT)) {
                showFieldError(etArr, getString(R.string.trip_detail_error_arrival_invalid_format));
                return;
            }
            if (!ValidationUtils.isAfterOrEqualCurrentMoment(arr, DATETIME_FORMAT)) {
                showFieldError(etArr, getString(R.string.trip_detail_error_arrival_past));
                return;
            }

            if (!ValidationUtils.isNullOrEmpty(dep)) {
                if (!ValidationUtils.isValidDate(dep, DATETIME_FORMAT)) {
                    showFieldError(etDep, getString(R.string.trip_detail_error_departure_invalid_format));
                    return;
                }
                if (!ValidationUtils.isStartBeforeOrEqualEnd(arr, dep, DATETIME_FORMAT)) {
                    showFieldError(etDep, getString(R.string.trip_detail_error_departure_before_arrival));
                    return;
                }
            }

            List<String> stopNames = new ArrayList<>();
            for (RouteStop stop : stopList) {
                stopNames.add(stop.getLocationName());
            }
            if (ValidationUtils.isDuplicateName(name, stopNames)) {
                Toast.makeText(requireContext(), getString(R.string.trip_detail_toast_duplicate_stop), Toast.LENGTH_SHORT).show();
                return;
            }

            int nextOrder = stopList.size() + 1;
            RouteStop stop = new RouteStop(currentTripId, name, nextOrder, arr, dep);

            executorService.execute(() -> {
                database.routeStopDao().insert(stop);
                loadRouteStops();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.trip_detail_toast_stop_added), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        dialog.show();
    }

    private void pickDateTime(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) ->
                new TimePickerDialog(requireContext(), (timeView, hourOfDay, minute) ->
                        editText.setText(String.format("%02d:%02d %02d/%02d/%04d", hourOfDay, minute, dayOfMonth, month + 1, year)),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                ).show(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void clearErrors(EditText... fields) {
        for (EditText field : fields) {
            if (field != null) {
                field.setError(null);
            }
        }
    }

    private void showFieldError(EditText field, String message) {
        if (field != null) {
            field.setError(message);
            field.requestFocus();
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadTripDetails() {
        executorService.execute(() -> {
            currentTrip = database.tripDao().getTripById(currentTripId);
            if (currentTrip == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), getString(R.string.trip_detail_toast_trip_not_exists), Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
                }
                return;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTripName.setText(currentTrip.getName());
                    tvDate.setText(getString(
                            R.string.trip_detail_date_range_format,
                            currentTrip.getStartDate(),
                            currentTrip.getEndDate()
                    ));
                    updateTabUI(currentTrip.getStatus());
                });
            }
        });
    }

    private void loadRouteStops() {
        executorService.execute(() -> {
            List<RouteStop> dbStops = database.routeStopDao().getStopsByTripId(currentTripId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    stopList.clear();
                    stopList.addAll(dbStops);
                    adapter.setStops(stopList);
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
