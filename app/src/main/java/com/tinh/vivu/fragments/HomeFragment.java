package com.tinh.vivu.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.models.Vehicle;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.TripAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    private static final int MIN_TRIP_NAME_LENGTH = 3;
    private static final int MAX_TRIP_NAME_LENGTH = 60;
    private static final int MIN_ODOMETER = 0;
    private static final int MAX_ODOMETER = 9_999_999;
    private static final double MAX_BUDGET = 1_000_000_000_000d;

    private RecyclerView rvTrips;
    private ImageView btnAddTrip;
    private AppDatabase database;
    private ExecutorService executorService;
    private List<Trip> tripList;
    private TripAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTrips = view.findViewById(R.id.rv_trips);
        btnAddTrip = view.findViewById(R.id.btn_add_trip);

        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        tripList = new ArrayList<>();

        adapter = new TripAdapter();
        rvTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTrips.setAdapter(adapter);

        adapter.setOnTripClickListener(new TripAdapter.OnTripClickListener() {
            @Override
            public void onDeleteClick(Trip trip) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.home_delete_trip_title)
                        .setMessage(R.string.home_delete_trip_message)
                        .setPositiveButton(R.string.home_yes, (dialog, which) -> {
                            executorService.execute(() -> {
                                database.tripDao().delete(trip);
                                loadTrips();
                            });
                        })
                        .setNegativeButton(R.string.home_no, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onEditClick(Trip trip) {
                showTripDialog(trip);
            }

            @Override
            public void onTripClick(Trip trip) {
                FragmentManager fragmentManager = getParentFragmentManager();
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, TripDetailFragment.newInstance(trip.getId()))
                        .addToBackStack("trip_detail")
                        .commit();
            }
        });

        btnAddTrip.setOnClickListener(v -> showTripDialog(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        executorService.execute(() -> {
            List<Trip> dbTrips = database.tripDao().getAllTrips();

            if (dbTrips.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    dbTrips = database.tripDao().getAllTrips();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            List<Trip> finalTrips = dbTrips;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tripList.clear();
                    tripList.addAll(finalTrips);
                    adapter.setTrips(tripList);
                });
            }
        });
    }

    private void showTripDialog(Trip tripToEdit) {
        executorService.execute(() -> {
            List<Vehicle> vehicles = database.vehicleDao().getAllVehicles();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showTripDialogContent(tripToEdit, vehicles));
            }
        });
    }

    private void showTripDialogContent(Trip tripToEdit, List<Vehicle> dbVehicles) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_trip);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        EditText etName = dialog.findViewById(R.id.et_trip_name);
        EditText etStart = dialog.findViewById(R.id.et_start_date);
        EditText etEnd = dialog.findViewById(R.id.et_end_date);
        EditText etBudget = dialog.findViewById(R.id.et_budget);
        Spinner spinnerVehicle = dialog.findViewById(R.id.spinner_trip_vehicle);
        TextView tvManageVehicle = dialog.findViewById(R.id.tv_manage_vehicle_trip);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_create_trip);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        List<Vehicle> vehicles = new ArrayList<>();
        List<String> vehicleOptions = new ArrayList<>();
        vehicleOptions.add(getString(R.string.home_vehicle_not_selected));
        vehicles.addAll(dbVehicles);
        for (Vehicle vehicle : vehicles) {
            vehicleOptions.add(vehicle.toString());
        }
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, vehicleOptions);
        spinnerVehicle.setAdapter(vehicleAdapter);

        if (tripToEdit != null) {
            if (tvTitle != null) tvTitle.setText(getString(R.string.home_trip_dialog_edit_title));
            btnAction.setText(getString(R.string.common_update));
            etName.setText(tripToEdit.getName());
            etStart.setText(tripToEdit.getStartDate());
            etEnd.setText(tripToEdit.getEndDate());
            etBudget.setText(tripToEdit.getTotalBudget() == 0 ? "" : String.valueOf((long)tripToEdit.getTotalBudget()));
            if (tripToEdit.getVehicleId() != null) {
                for (int i = 0; i < vehicles.size(); i++) {
                    if (vehicles.get(i).getVehicleId() == tripToEdit.getVehicleId()) {
                        spinnerVehicle.setSelection(i + 1);
                        break;
                    }
                }
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        tvManageVehicle.setOnClickListener(v -> showQuickAddVehicleDialog(vehicles, vehicleOptions, vehicleAdapter));

        etStart.setOnClickListener(v -> showDatePicker(etStart));
        etEnd.setOnClickListener(v -> showDatePicker(etEnd));

        btnAction.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String start = etStart.getText().toString().trim();
            String end = etEnd.getText().toString().trim();
            String budgetStr = etBudget.getText().toString().trim();

            clearErrors(etName, etStart, etEnd, etBudget);

            if (ValidationUtils.isNullOrEmpty(name)) {
                showFieldError(etName, getString(R.string.home_error_trip_name_required));
                return;
            }
            if (!ValidationUtils.isValidDisplayName(name, MIN_TRIP_NAME_LENGTH, MAX_TRIP_NAME_LENGTH)) {
                showFieldError(etName, getString(R.string.home_error_trip_name_invalid));
                return;
            }
            if (ValidationUtils.isNullOrEmpty(start)) {
                showFieldError(etStart, getString(R.string.home_error_start_date_required));
                return;
            }
            if (!ValidationUtils.isValidDate(start, "dd/MM/yyyy")) {
                showFieldError(etStart, getString(R.string.home_error_start_date_invalid_format));
                return;
            }
            boolean isStartChanged = tripToEdit == null || !start.equals(tripToEdit.getStartDate());
            if (isStartChanged && !ValidationUtils.isAfterOrEqualCurrentDate(start, "dd/MM/yyyy")) {
                showFieldError(etStart, getString(R.string.home_error_start_date_past));
                return;
            }
            if (!ValidationUtils.isNullOrEmpty(end)) {
                if (!ValidationUtils.isValidDate(end, "dd/MM/yyyy")) {
                    showFieldError(etEnd, getString(R.string.home_error_end_date_invalid_format));
                    return;
                }
                if (!ValidationUtils.isStartBeforeOrEqualEnd(start, end, "dd/MM/yyyy")) {
                    showFieldError(etEnd, getString(R.string.home_error_end_date_before_start));
                    return;
                }
            }

            double tempBudget = 0;
            if (!ValidationUtils.isNullOrEmpty(budgetStr)) {
                if (!ValidationUtils.isInteger(budgetStr)) {
                    showFieldError(etBudget, getString(R.string.home_error_budget_integer));
                    return;
                }
                if (!ValidationUtils.isNumericInRange(budgetStr, 0, MAX_BUDGET)) {
                    showFieldError(etBudget, getString(R.string.home_error_budget_out_of_range));
                    return;
                }
                tempBudget = Double.parseDouble(budgetStr);
            }
            final double finalBudget = tempBudget;
            Integer selectedVehicleId = null;
            int vehiclePos = spinnerVehicle.getSelectedItemPosition();
            if (vehiclePos > 0) {
                selectedVehicleId = vehicles.get(vehiclePos - 1).getVehicleId();
            }
            final Integer finalVehicleId = selectedVehicleId;

            executorService.execute(() -> {
                if (tripToEdit == null) {
                    Trip newTrip = new Trip(name, start, end, finalBudget, getString(R.string.home_trip_status_planning_default));
                    newTrip.setVehicleId(finalVehicleId);
                    database.tripDao().insert(newTrip);
                } else {
                    tripToEdit.setName(name);
                    tripToEdit.setStartDate(start);
                    tripToEdit.setEndDate(end);
                    tripToEdit.setTotalBudget(finalBudget);
                    tripToEdit.setVehicleId(finalVehicleId);
                    database.tripDao().update(tripToEdit);
                }

                loadTrips();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), tripToEdit == null
                                ? getString(R.string.home_toast_trip_created)
                                : getString(R.string.home_toast_trip_updated), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        dialog.show();
    }

    private void showQuickAddVehicleDialog(List<Vehicle> vehicles, List<String> vehicleOptions, ArrayAdapter<String> vehicleAdapter) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manage_vehicle);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        Spinner spinnerExisting = dialog.findViewById(R.id.spinner_vehicle_existing);
        MaterialButton btnAssign = dialog.findViewById(R.id.btn_assign_vehicle);
        EditText etPlate = dialog.findViewById(R.id.et_vehicle_plate);
        EditText etInitialOdo = dialog.findViewById(R.id.et_vehicle_initial_odo);
        EditText etCurrentOdo = dialog.findViewById(R.id.et_vehicle_current_odo);
        MaterialButton btnSaveVehicle = dialog.findViewById(R.id.btn_save_vehicle);
        MaterialButton btnCloseDialog = dialog.findViewById(R.id.btn_close_vehicle_dialog);

        ArrayAdapter<String> existingAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, vehicleOptions);
        spinnerExisting.setAdapter(existingAdapter);

        btnAssign.setVisibility(View.GONE);
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        btnSaveVehicle.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            String initialOdoStr = etInitialOdo.getText().toString().trim();
            String currentOdoStr = etCurrentOdo.getText().toString().trim();

            clearErrors(etPlate, etInitialOdo, etCurrentOdo);

            if (ValidationUtils.isNullOrEmpty(plate)) {
                showFieldError(etPlate, getString(R.string.home_error_vehicle_plate_required));
                return;
            }
            if (!ValidationUtils.isValidVehiclePlate(plate)) {
                showFieldError(etPlate, getString(R.string.home_error_vehicle_plate_format));
                return;
            }
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getLicensePlate() != null
                        && vehicle.getLicensePlate().equalsIgnoreCase(plate)) {
                    showFieldError(etPlate, getString(R.string.home_error_vehicle_plate_exists));
                    return;
                }
            }
            if (!ValidationUtils.isIntegerInRange(initialOdoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etInitialOdo, getString(R.string.home_error_initial_odo_invalid));
                return;
            }
            if (!ValidationUtils.isIntegerInRange(currentOdoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etCurrentOdo, getString(R.string.home_error_current_odo_invalid));
                return;
            }

            int initialOdo = Integer.parseInt(initialOdoStr);
            int currentOdo = Integer.parseInt(currentOdoStr);
            if (currentOdo < initialOdo) {
                showFieldError(etCurrentOdo, getString(R.string.home_error_current_odo_less_than_initial));
                return;
            }

            executorService.execute(() -> {
                Vehicle vehicle = new Vehicle(plate.toUpperCase(Locale.getDefault()), initialOdo, currentOdo);
                long newVehicleId = database.vehicleDao().insert(vehicle);
                vehicle.setVehicleId((int) newVehicleId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        vehicles.add(vehicle);
                        vehicleOptions.add(vehicle.toString());
                        vehicleAdapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), getString(R.string.home_toast_vehicle_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        });

        dialog.show();
    }

    private void showDatePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            editText.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
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
}