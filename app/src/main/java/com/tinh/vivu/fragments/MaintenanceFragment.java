package com.tinh.vivu.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tinh.vivu.R;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.FuelLog;
import com.tinh.vivu.models.MaintenanceHistoryItem;
import com.tinh.vivu.models.MaintenanceLog;
import com.tinh.vivu.models.MaintenanceStatusItem;
import com.tinh.vivu.models.MaintenanceType;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.models.Vehicle;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.MaintenanceHistoryAdapter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaintenanceFragment extends Fragment {
    private static final int MIN_ODOMETER = 0;
    private static final int MAX_ODOMETER = 9_999_999;
    private static final int MAX_INTERVAL_KM = 100_000;
    private static final double MAX_CURRENCY_AMOUNT = 1_000_000_000_000d;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 60;
    private static final int MAX_TEXT_LENGTH = 120;
    private static final String PREFS_MAINTENANCE = "maintenance_prefs";
    private static final String PREF_KEY_FUEL_BASELINE_PREFIX = "fuel_baseline_vehicle_";

    private TextView tvTripName;
    private TextView tvVehiclePlate;
    private TextView tvVehicleEmpty;
    private TextView tvTotalDistance;
    private TextView tvAvgConsumption;
    private TextView tvDistanceSinceOil;
    private TextView tvOilMessage;
    private TextView tvLastOilChange;
    private TextView tvFuelConsumptionDetail;
    private TextView tvFuelSummary;
    private TextView tvKmPerLiter;
    private TextView tvLatestFuelSegment;
    private EditText etAddDistance;
    private ImageView ivOilStatus;
    private ProgressBar progressOilChange;
    private LinearLayout llMaintenanceChecklist;
    private RecyclerView rvMaintenanceHistory;
    private TextView tvHistoryEmpty;
    private MaterialButton btnAddDistance;
    private MaterialButton btnLogFuel;
    private MaterialButton btnLogMaintenance;
    private MaterialButton btnMarkOilChange;
    private MaterialButton btnManageVehicle;
    private MaterialButton btnManageIntervals;
    private MaterialButton btnResetFuelCycle;

    private AppDatabase database;
    private ExecutorService executorService;
    private MaintenanceHistoryAdapter historyAdapter;

    private final DecimalFormat currencyFormatter = new DecimalFormat("#,###");
    private final DecimalFormat fuelFormatter = new DecimalFormat("0.0");
    private final List<MaintenanceType> maintenanceTypes = new ArrayList<>();
    private final List<Vehicle> allVehicles = new ArrayList<>();

    private Trip activeTrip;
    private Vehicle currentVehicle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maintenance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        historyAdapter = new MaintenanceHistoryAdapter();
        rvMaintenanceHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMaintenanceHistory.setAdapter(historyAdapter);

        btnAddDistance.setOnClickListener(v -> updateDistance());
        btnLogFuel.setOnClickListener(v -> showFuelDialog());
        btnLogMaintenance.setOnClickListener(v -> showMaintenanceDialog());
        btnMarkOilChange.setOnClickListener(v -> markOilChangeComplete());
        btnManageVehicle.setOnClickListener(v -> showManageVehicleDialog());
        btnManageIntervals.setOnClickListener(v -> showManageIntervalsDialog());
        btnResetFuelCycle.setOnClickListener(v -> showResetFuelCycleDialog());

        loadMaintenanceData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (database != null && executorService != null && !executorService.isShutdown()) {
            loadMaintenanceData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void initViews(View view) {
        tvTripName = view.findViewById(R.id.tv_trip_name);
        tvVehiclePlate = view.findViewById(R.id.tv_vehicle_plate);
        tvVehicleEmpty = view.findViewById(R.id.tv_vehicle_empty);
        tvTotalDistance = view.findViewById(R.id.tv_total_distance);
        tvAvgConsumption = view.findViewById(R.id.tv_avg_consumption);
        tvDistanceSinceOil = view.findViewById(R.id.tv_distance_since_oil);
        tvOilMessage = view.findViewById(R.id.tv_oil_message);
        tvLastOilChange = view.findViewById(R.id.tv_last_oil_change);
        tvFuelConsumptionDetail = view.findViewById(R.id.tv_fuel_consumption_detail);
        tvFuelSummary = view.findViewById(R.id.tv_fuel_summary);
        tvKmPerLiter = view.findViewById(R.id.tv_km_per_liter);
        tvLatestFuelSegment = view.findViewById(R.id.tv_latest_fuel_segment);
        etAddDistance = view.findViewById(R.id.et_add_distance);
        ivOilStatus = view.findViewById(R.id.iv_oil_status);
        progressOilChange = view.findViewById(R.id.progress_oil_change);
        llMaintenanceChecklist = view.findViewById(R.id.ll_maintenance_checklist);
        rvMaintenanceHistory = view.findViewById(R.id.rv_maintenance_history);
        tvHistoryEmpty = view.findViewById(R.id.tv_history_empty);
        btnAddDistance = view.findViewById(R.id.btn_add_distance);
        btnLogFuel = view.findViewById(R.id.btn_log_fuel);
        btnLogMaintenance = view.findViewById(R.id.btn_log_maintenance);
        btnMarkOilChange = view.findViewById(R.id.btn_mark_oil_change);
        btnManageVehicle = view.findViewById(R.id.btn_manage_vehicle);
        btnManageIntervals = view.findViewById(R.id.btn_manage_intervals);
        btnResetFuelCycle = view.findViewById(R.id.btn_reset_fuel_cycle);
    }

    private void loadMaintenanceData() {
        Context appContext = requireContext().getApplicationContext();
        executorService.execute(() -> {
            Trip latestTrip = database.tripDao().getLatestTrip();
            List<Vehicle> vehicles = database.vehicleDao().getAllVehicles();
            Vehicle vehicle = null;

            if (latestTrip != null && latestTrip.getVehicleId() != null) {
                vehicle = database.vehicleDao().getVehicleById(latestTrip.getVehicleId());
            }

            if (vehicle == null && !vehicles.isEmpty()) {
                vehicle = vehicles.get(0);
                if (latestTrip != null && latestTrip.getVehicleId() == null) {
                    latestTrip.setVehicleId(vehicle.getVehicleId());
                    database.tripDao().update(latestTrip);
                }
            }

            List<MaintenanceType> loadedTypes = database.maintenanceDao().getAllMaintenanceTypes();
            List<MaintenanceStatusItem> statusItems = new ArrayList<>();
            List<MaintenanceHistoryItem> historyItems = new ArrayList<>();
            MaintenanceLog latestOilLog = null;
            FuelStats fuelStats = new FuelStats();

            if (vehicle != null) {
                statusItems = database.maintenanceDao().getMaintenanceStatusForVehicle(vehicle.getVehicleId());
                historyItems = database.maintenanceDao().getMaintenanceHistoryForVehicle(vehicle.getVehicleId());
                latestOilLog = database.maintenanceDao().getLatestOilChangeLog(vehicle.getVehicleId());
                int fuelBaselineOdo = getFuelBaselineOdometer(appContext, vehicle);
                fuelStats = calculateFuelStats(database.fuelLogDao().getFuelLogsByVehicle(vehicle.getVehicleId()), fuelBaselineOdo);
            }

            Trip finalTrip = latestTrip;
            Vehicle finalVehicle = vehicle;
            List<Vehicle> finalVehicles = vehicles;
            List<MaintenanceType> finalLoadedTypes = loadedTypes;
            List<MaintenanceStatusItem> finalStatusItems = statusItems;
            List<MaintenanceHistoryItem> finalHistoryItems = historyItems;
            MaintenanceLog finalLatestOilLog = latestOilLog;
            FuelStats finalFuelStats = fuelStats;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderData(
                        finalTrip,
                        finalVehicle,
                        finalVehicles,
                        finalLoadedTypes,
                        finalStatusItems,
                        finalHistoryItems,
                        finalLatestOilLog,
                        finalFuelStats
                ));
            }
        });
    }

    private void renderData(@Nullable Trip trip,
                            @Nullable Vehicle vehicle,
                            List<Vehicle> vehicles,
                            List<MaintenanceType> loadedTypes,
                            List<MaintenanceStatusItem> statusItems,
                            List<MaintenanceHistoryItem> historyItems,
                            @Nullable MaintenanceLog latestOilLog,
                            FuelStats fuelStats) {
        activeTrip = trip;
        currentVehicle = vehicle;
        maintenanceTypes.clear();
        maintenanceTypes.addAll(loadedTypes);
        allVehicles.clear();
        allVehicles.addAll(vehicles);

        boolean hasVehicle = vehicle != null;
        boolean hasTrip = trip != null;

        tvVehicleEmpty.setVisibility(hasVehicle ? View.GONE : View.VISIBLE);
        btnAddDistance.setEnabled(hasVehicle);
        btnLogMaintenance.setEnabled(hasVehicle);
        btnMarkOilChange.setEnabled(hasVehicle);
        btnLogFuel.setEnabled(hasVehicle && hasTrip);
        btnResetFuelCycle.setEnabled(hasVehicle);
        btnManageIntervals.setEnabled(true);

        if (!hasVehicle) {
            tvTripName.setText(hasTrip
                    ? getString(R.string.maintenance_trip_label_format, trip.getName())
                    : getString(R.string.maintenance_trip_none));
            tvVehiclePlate.setText(getString(R.string.maintenance_vehicle_unlinked));
            tvTotalDistance.setText("0");
            tvAvgConsumption.setText("0.0");
            tvDistanceSinceOil.setText("0");
            tvFuelConsumptionDetail.setText(getString(R.string.maintenance_avg_consumption_default));
            tvKmPerLiter.setText(getString(R.string.maintenance_km_per_liter_default));
            tvFuelSummary.setText(getString(R.string.maintenance_need_vehicle_for_metrics));
            tvLatestFuelSegment.setText(getString(R.string.maintenance_latest_segment_default));
            tvOilMessage.setText(getString(R.string.maintenance_oil_no_data));
            tvLastOilChange.setText(getString(R.string.maintenance_last_oil_none));
            progressOilChange.setProgress(0);
            llMaintenanceChecklist.removeAllViews();
            addChecklistPlaceholder(getString(R.string.maintenance_checklist_no_vehicle));
            historyAdapter.setItems(new ArrayList<>());
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvTripName.setText(hasTrip
                ? getString(R.string.maintenance_trip_label_format, trip.getName())
                : getString(R.string.maintenance_trip_unlinked));
        tvVehiclePlate.setText(getString(R.string.maintenance_vehicle_plate_format, vehicle.getLicensePlate()));
        tvTotalDistance.setText(String.valueOf(vehicle.getCurrentOdometer()));

        MaintenanceType oilType = findOilType(loadedTypes);
        int oilInterval = oilType != null ? oilType.getIntervalKm() : 2000;
        int lastOilOdo = latestOilLog != null ? latestOilLog.getOdoAtMaint() : vehicle.getInitialOdometer();
        int distanceSinceOil = Math.max(0, vehicle.getCurrentOdometer() - lastOilOdo);
        int oilChangeDue = oilInterval - distanceSinceOil;
        int oilProgress = oilInterval > 0 ? Math.min((distanceSinceOil * 100) / oilInterval, 100) : 0;

        tvDistanceSinceOil.setText(String.valueOf(distanceSinceOil));
        progressOilChange.setProgressBackgroundTintMode(PorterDuff.Mode.SRC);
        progressOilChange.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
        progressOilChange.setProgress(oilProgress);

        if (oilChangeDue > 0) {
            ivOilStatus.setImageResource(android.R.drawable.checkbox_on_background);
            ivOilStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
            progressOilChange.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
            tvOilMessage.setText(getString(R.string.maintenance_oil_due_in_km, oilChangeDue));
        } else {
            ivOilStatus.setImageResource(android.R.drawable.ic_dialog_alert);
            ivOilStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
            progressOilChange.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
            tvOilMessage.setText(getString(R.string.maintenance_oil_overdue_km, Math.abs(oilChangeDue)));
        }

        if (latestOilLog != null) {
            tvLastOilChange.setText(getString(
                    R.string.maintenance_last_oil_change_format,
                    latestOilLog.getOdoAtMaint(),
                    latestOilLog.getDate()
            ));
        } else {
            tvLastOilChange.setText(getString(
                    R.string.maintenance_last_oil_initial_marker_format,
                    vehicle.getInitialOdometer()
            ));
        }

        double primaryLPer100 = fuelStats.hasLatestSegment ? fuelStats.latestSegmentLPer100 : 0;
        double primaryKmPerLiter = fuelStats.hasLatestSegment ? fuelStats.latestSegmentKmPerLiter : 0;

        tvAvgConsumption.setText(fuelFormatter.format(primaryLPer100));
        tvFuelConsumptionDetail.setText(getString(
                R.string.maintenance_consumption_l_per_100km_format,
                fuelFormatter.format(primaryLPer100)
        ));
        tvKmPerLiter.setText(getString(
                R.string.maintenance_km_per_liter_format,
                fuelFormatter.format(primaryKmPerLiter)
        ));

        if (fuelStats.hasLatestSegment) {
            tvFuelSummary.setText(getString(
                    R.string.maintenance_fuel_summary_latest_pair,
                    fuelStats.latestSegmentStartOdo,
                    fuelStats.latestFuelOdo
            ));
            tvLatestFuelSegment.setText(getString(
                    R.string.maintenance_latest_segment_format,
                    Math.round(fuelStats.latestSegmentDistance),
                    fuelFormatter.format(fuelStats.latestSegmentKmPerLiter),
                    fuelFormatter.format(fuelStats.latestSegmentLPer100),
                    fuelFormatter.format(fuelStats.latestSegmentLiters)
            ));
        } else if (fuelStats.logCount > 0) {
            tvFuelSummary.setText(getString(R.string.maintenance_fuel_summary_need_more_logs));
            tvLatestFuelSegment.setText(getString(R.string.maintenance_latest_segment_default));
        } else {
            tvFuelSummary.setText(getString(R.string.maintenance_fuel_summary_no_data));
            tvLatestFuelSegment.setText(getString(R.string.maintenance_latest_segment_default));
        }

        renderChecklist(statusItems, vehicle.getCurrentOdometer(), vehicle.getInitialOdometer());
        historyAdapter.setItems(historyItems);
        tvHistoryEmpty.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderChecklist(List<MaintenanceStatusItem> statusItems, int currentOdometer, int initialOdometer) {
        llMaintenanceChecklist.removeAllViews();

        if (statusItems == null || statusItems.isEmpty()) {
            addChecklistPlaceholder(getString(R.string.maintenance_checklist_no_types));
            return;
        }

        for (MaintenanceStatusItem item : statusItems) {
            int lastOdo = item.getLastMaintenanceOdo() != null ? item.getLastMaintenanceOdo() : initialOdometer;
            int distanceSinceLast = Math.max(0, currentOdometer - lastOdo);
            int remaining = item.getIntervalKm() - distanceSinceLast;
            boolean isDue = remaining <= 0;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#F8FAFC"));
            bg.setCornerRadius(dp(12));
            row.setBackground(bg);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dp(10);
            row.setLayoutParams(rowParams);

            LinearLayout textWrap = new LinearLayout(requireContext());
            textWrap.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textWrap.setLayoutParams(textParams);

            TextView tvName = new TextView(requireContext());
            tvName.setText(item.getName());
            tvName.setTextColor(Color.BLACK);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(isDue
                    ? getString(R.string.maintenance_checklist_due_format, Math.abs(remaining), item.getIntervalKm())
                    : getString(R.string.maintenance_checklist_remaining_format, remaining, item.getIntervalKm()));
            tvSub.setTextColor(Color.parseColor("#667085"));
            tvSub.setTextSize(12);
            tvSub.setPadding(0, dp(4), 0, 0);

            textWrap.addView(tvName);
            textWrap.addView(tvSub);

            ImageView statusIcon = new ImageView(requireContext());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            statusIcon.setLayoutParams(iconParams);
            statusIcon.setImageResource(isDue ? android.R.drawable.ic_dialog_alert : android.R.drawable.checkbox_on_background);
            statusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(isDue ? "#F59E0B" : "#16A34A")));

            row.addView(textWrap);
            row.addView(statusIcon);
            llMaintenanceChecklist.addView(row);
        }
    }

    private void addChecklistPlaceholder(String message) {
        TextView emptyView = new TextView(requireContext());
        emptyView.setText(message);
        emptyView.setTextColor(Color.parseColor("#667085"));
        emptyView.setTextSize(14);
        llMaintenanceChecklist.addView(emptyView);
    }

    private void updateDistance() {
        if (currentVehicle == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_vehicle_update_odo), Toast.LENGTH_SHORT).show();
            return;
        }

        String distanceStr = etAddDistance.getText().toString().trim();
        clearErrors(etAddDistance);
        if (!ValidationUtils.isIntegerInRange(distanceStr, MIN_ODOMETER, MAX_ODOMETER)) {
            showFieldError(etAddDistance, getString(R.string.maintenance_error_valid_km_input));
            return;
        }

        int newOdometer = Integer.parseInt(distanceStr);
        if (newOdometer < currentVehicle.getCurrentOdometer()) {
            showFieldError(etAddDistance, getString(R.string.maintenance_error_odo_less_than_current));
            return;
        }

        int vehicleId = currentVehicle.getVehicleId();
        executorService.execute(() -> {
            Vehicle latestVehicle = database.vehicleDao().getVehicleById(vehicleId);
            if (latestVehicle == null) {
                showToastOnUiThread(getString(R.string.maintenance_toast_vehicle_not_found_update));
                return;
            }

            if (newOdometer < latestVehicle.getCurrentOdometer()) {
                showToastOnUiThread(getString(R.string.maintenance_error_odo_less_than_current));
                return;
            }

            latestVehicle.setCurrentOdometer(newOdometer);
            database.vehicleDao().update(latestVehicle);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etAddDistance.setText("");
                    Toast.makeText(requireContext(), getString(R.string.maintenance_toast_vehicle_odo_updated), Toast.LENGTH_SHORT).show();
                    loadMaintenanceData();
                });
            }
        });
    }

    private void markOilChangeComplete() {
        if (currentVehicle == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_vehicle_mark_oil), Toast.LENGTH_SHORT).show();
            return;
        }

        MaintenanceType oilType = findOilType(maintenanceTypes);
        if (oilType == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_oil_type), Toast.LENGTH_SHORT).show();
            return;
        }

        int vehicleId = currentVehicle.getVehicleId();
        int currentOdometer = currentVehicle.getCurrentOdometer();
        executorService.execute(() -> {
            database.maintenanceDao().insertLog(new MaintenanceLog(
                    vehicleId,
                    oilType.getTypeId(),
                    currentOdometer,
                    0,
                    getCurrentDate(),
                    getString(R.string.maintenance_oil_quick_note)
            ));

            showToastOnUiThread(getString(R.string.maintenance_toast_oil_logged));
            loadMaintenanceData();
        });
    }

    private void showFuelDialog() {
        if (currentVehicle == null || activeTrip == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_need_trip_vehicle_for_fuel), Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_log_fuel);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etFuelOdo = dialog.findViewById(R.id.et_fuel_odo);
        EditText etFuelPrice = dialog.findViewById(R.id.et_fuel_price);
        EditText etFuelTotal = dialog.findViewById(R.id.et_fuel_total);
        EditText etFuelLocation = dialog.findViewById(R.id.et_fuel_location);
        TextView tvFuelCycleBaseline = dialog.findViewById(R.id.tv_fuel_cycle_baseline);
        TextView tvLitersPreview = dialog.findViewById(R.id.tv_fuel_liters_preview);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save_fuel);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_fuel);

        int fuelBaselineOdo = getFuelBaselineOdometer(requireContext().getApplicationContext(), currentVehicle);
        tvFuelCycleBaseline.setText(getString(R.string.maintenance_fuel_cycle_baseline_format, fuelBaselineOdo));
        etFuelOdo.setText(String.valueOf(currentVehicle.getCurrentOdometer()));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextWatcher previewWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateLitersPreview(etFuelPrice, etFuelTotal, tvLitersPreview);
            }
        };
        etFuelPrice.addTextChangedListener(previewWatcher);
        etFuelTotal.addTextChangedListener(previewWatcher);

        int tripId = activeTrip.getId();
        int vehicleId = currentVehicle.getVehicleId();
        int currentOdometer = currentVehicle.getCurrentOdometer();

        btnSave.setOnClickListener(v -> {
            String odoStr = etFuelOdo.getText().toString().trim();
            String priceStr = etFuelPrice.getText().toString().trim();
            String totalStr = etFuelTotal.getText().toString().trim();
            String location = etFuelLocation.getText().toString().trim();

            clearErrors(etFuelOdo, etFuelPrice, etFuelTotal, etFuelLocation);

            if (!ValidationUtils.isIntegerInRange(odoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etFuelOdo, getString(R.string.maintenance_error_odo_invalid_integer));
                return;
            }
            if (!ValidationUtils.isValidDecimalFormat(priceStr, 12, 2) || !ValidationUtils.isNumeric(priceStr)) {
                showFieldError(etFuelPrice, getString(R.string.maintenance_error_fuel_price_invalid_format));
                return;
            }
            if (!ValidationUtils.isValidDecimalFormat(totalStr, 12, 2) || !ValidationUtils.isNumeric(totalStr)) {
                showFieldError(etFuelTotal, getString(R.string.maintenance_error_fuel_total_invalid_format));
                return;
            }

            int odo = Integer.parseInt(odoStr);
            double pricePerLiter = Double.parseDouble(priceStr);
            double totalCost = Double.parseDouble(totalStr);

            if (!ValidationUtils.isNumberInRange(pricePerLiter, 0.01d, MAX_CURRENCY_AMOUNT)) {
                showFieldError(etFuelPrice, getString(R.string.maintenance_error_fuel_price_positive));
                return;
            }
            if (!ValidationUtils.isNumberInRange(totalCost, 0.01d, MAX_CURRENCY_AMOUNT)) {
                showFieldError(etFuelTotal, getString(R.string.maintenance_error_fuel_total_positive));
                return;
            }
            if (!ValidationUtils.isNullOrEmpty(location)
                    && !ValidationUtils.isValidDisplayName(location, MIN_NAME_LENGTH, 80)) {
                showFieldError(etFuelLocation, getString(R.string.maintenance_error_location_invalid));
                return;
            }

            double liters = totalCost / pricePerLiter;

            if (odo < currentOdometer) {
                showFieldError(etFuelOdo, getString(R.string.maintenance_error_odo_less_than_current));
                return;
            }

            executorService.execute(() -> {
                Vehicle latestVehicle = database.vehicleDao().getVehicleById(vehicleId);
                if (latestVehicle == null) {
                    showToastOnUiThread(getString(R.string.maintenance_toast_vehicle_not_found_fuel));
                    return;
                }

                database.fuelLogDao().insert(new FuelLog(
                        tripId,
                        vehicleId,
                        odo,
                        liters,
                        pricePerLiter,
                        totalCost,
                        location.isEmpty() ? getString(R.string.maintenance_location_unknown) : location,
                        getCurrentDate()
                ));

                if (odo > latestVehicle.getCurrentOdometer()) {
                    latestVehicle.setCurrentOdometer(odo);
                    database.vehicleDao().update(latestVehicle);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_fuel_saved), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        });

        dialog.show();
    }

    private void showResetFuelCycleDialog() {
        if (currentVehicle == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_vehicle_update_odo), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.maintenance_reset_fuel_cycle_title)
                .setMessage(getString(
                        R.string.maintenance_reset_fuel_cycle_message,
                        currentVehicle.getCurrentOdometer()
                ))
                .setPositiveButton(R.string.common_reset, (dialogInterface, i) -> resetFuelCycle())
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void resetFuelCycle() {
        if (currentVehicle == null) {
            return;
        }

        int baselineOdo = currentVehicle.getCurrentOdometer();
        saveFuelBaselineOdometer(
                requireContext().getApplicationContext(),
                currentVehicle.getVehicleId(),
                baselineOdo
        );
        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_fuel_cycle_reset), Toast.LENGTH_SHORT).show();
        loadMaintenanceData();
    }

    private void updateLitersPreview(EditText etFuelPrice, EditText etFuelTotal, TextView tvLitersPreview) {
        String priceStr = etFuelPrice.getText().toString().trim();
        String totalStr = etFuelTotal.getText().toString().trim();
        if (!ValidationUtils.isNumeric(priceStr) || !ValidationUtils.isNumeric(totalStr)) {
            tvLitersPreview.setText(getString(R.string.maintenance_fuel_liters_auto));
            return;
        }

        double price = Double.parseDouble(priceStr);
        double total = Double.parseDouble(totalStr);
        if (price <= 0 || total <= 0) {
            tvLitersPreview.setText(getString(R.string.maintenance_fuel_liters_auto));
            return;
        }

        double liters = total / price;
        tvLitersPreview.setText(getString(R.string.maintenance_fuel_liters_estimate_format, fuelFormatter.format(liters)));
    }

    private void showMaintenanceDialog() {
        if (currentVehicle == null) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_vehicle_maintenance), Toast.LENGTH_SHORT).show();
            return;
        }

        if (maintenanceTypes.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_maintenance_types_available), Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_log_maintenance);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerType = dialog.findViewById(R.id.spinner_maintenance_type);
        EditText etOdo = dialog.findViewById(R.id.et_maintenance_odo);
        EditText etCost = dialog.findViewById(R.id.et_maintenance_cost);
        EditText etNote = dialog.findViewById(R.id.et_maintenance_note);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save_maintenance);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_maintenance);

        ArrayAdapter<MaintenanceType> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                maintenanceTypes
        );
        spinnerType.setAdapter(adapter);
        etOdo.setText(String.valueOf(currentVehicle.getCurrentOdometer()));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        int vehicleId = currentVehicle.getVehicleId();
        int currentOdometer = currentVehicle.getCurrentOdometer();

        btnSave.setOnClickListener(v -> {
            String odoStr = etOdo.getText().toString().trim();
            String costStr = etCost.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            clearErrors(etOdo, etCost, etNote);

            if (!ValidationUtils.isIntegerInRange(odoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etOdo, getString(R.string.maintenance_error_odo_invalid_integer));
                return;
            }

            int odo = Integer.parseInt(odoStr);
            if (odo < currentOdometer) {
                showFieldError(etOdo, getString(R.string.maintenance_error_odo_less_than_current));
                return;
            }

            double cost = 0;
            if (!costStr.isEmpty()) {
                if (!ValidationUtils.isValidDecimalFormat(costStr, 12, 2) || !ValidationUtils.isNumeric(costStr)) {
                    showFieldError(etCost, getString(R.string.maintenance_error_cost_invalid_number));
                    return;
                }
                cost = Double.parseDouble(costStr);
                if (!ValidationUtils.isNumberInRange(cost, 0, MAX_CURRENCY_AMOUNT)) {
                    showFieldError(etCost, getString(R.string.maintenance_error_cost_out_of_range));
                    return;
                }
            }
            if (!ValidationUtils.isNullOrEmpty(note)
                    && !ValidationUtils.isValidDisplayName(note, MIN_NAME_LENGTH, MAX_TEXT_LENGTH)) {
                showFieldError(etNote, getString(R.string.maintenance_error_note_invalid));
                return;
            }

            MaintenanceType selectedType = (MaintenanceType) spinnerType.getSelectedItem();
            if (selectedType == null) {
                Toast.makeText(requireContext(), getString(R.string.maintenance_error_select_maintenance_type), Toast.LENGTH_SHORT).show();
                return;
            }

            double finalCost = cost;
            executorService.execute(() -> {
                Vehicle latestVehicle = database.vehicleDao().getVehicleById(vehicleId);
                if (latestVehicle == null) {
                    showToastOnUiThread(getString(R.string.maintenance_toast_vehicle_not_found_maintenance));
                    return;
                }

                database.maintenanceDao().insertLog(new MaintenanceLog(
                        vehicleId,
                        selectedType.getTypeId(),
                        odo,
                        finalCost,
                        getCurrentDate(),
                        note
                ));

                if (odo > latestVehicle.getCurrentOdometer()) {
                    latestVehicle.setCurrentOdometer(odo);
                    database.vehicleDao().update(latestVehicle);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_maintenance_saved), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        });

        dialog.show();
    }

    private void showManageVehicleDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manage_vehicle);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerExisting = dialog.findViewById(R.id.spinner_vehicle_existing);
        MaterialButton btnAssign = dialog.findViewById(R.id.btn_assign_vehicle);
        MaterialButton btnEditVehicle = dialog.findViewById(R.id.btn_edit_vehicle);
        MaterialButton btnDeleteVehicle = dialog.findViewById(R.id.btn_delete_vehicle);
        EditText etPlate = dialog.findViewById(R.id.et_vehicle_plate);
        EditText etInitialOdo = dialog.findViewById(R.id.et_vehicle_initial_odo);
        EditText etCurrentOdo = dialog.findViewById(R.id.et_vehicle_current_odo);
        TextView tvVehicleFormMode = dialog.findViewById(R.id.tv_vehicle_form_mode);
        MaterialButton btnSaveVehicle = dialog.findViewById(R.id.btn_save_vehicle);
        MaterialButton btnCloseDialog = dialog.findViewById(R.id.btn_close_vehicle_dialog);

        List<String> vehicleOptions = new ArrayList<>();
        vehicleOptions.add(getString(R.string.maintenance_vehicle_option_select));
        for (Vehicle vehicle : allVehicles) {
            vehicleOptions.add(vehicle.toString());
        }
        ArrayAdapter<String> existingAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, vehicleOptions);
        spinnerExisting.setAdapter(existingAdapter);

        btnAssign.setEnabled(activeTrip != null && !allVehicles.isEmpty());
        btnEditVehicle.setEnabled(!allVehicles.isEmpty());
        btnDeleteVehicle.setEnabled(!allVehicles.isEmpty());
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        final Vehicle[] editingVehicle = {null};

        btnAssign.setOnClickListener(v -> {
            int position = spinnerExisting.getSelectedItemPosition();
            if (activeTrip == null) {
                Toast.makeText(requireContext(), getString(R.string.maintenance_toast_no_trip_assign_vehicle), Toast.LENGTH_SHORT).show();
                return;
            }
            if (position <= 0) {
                Toast.makeText(requireContext(), getString(R.string.maintenance_error_select_vehicle_assign), Toast.LENGTH_SHORT).show();
                return;
            }

            Vehicle selectedVehicle = allVehicles.get(position - 1);
            executorService.execute(() -> {
                Trip latestTrip = database.tripDao().getTripById(activeTrip.getId());
                if (latestTrip == null) {
                    showToastOnUiThread(getString(R.string.maintenance_toast_trip_not_found_assign_vehicle));
                    return;
                }

                latestTrip.setVehicleId(selectedVehicle.getVehicleId());
                database.tripDao().update(latestTrip);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_vehicle_assigned_trip), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        });

        btnEditVehicle.setOnClickListener(v -> {
            int position = spinnerExisting.getSelectedItemPosition();
            if (position <= 0) {
                Toast.makeText(requireContext(), getString(R.string.maintenance_error_select_vehicle_edit), Toast.LENGTH_SHORT).show();
                return;
            }

            Vehicle selectedVehicle = allVehicles.get(position - 1);
            editingVehicle[0] = selectedVehicle;
            etPlate.setText(selectedVehicle.getLicensePlate());
            etInitialOdo.setText(String.valueOf(selectedVehicle.getInitialOdometer()));
            etCurrentOdo.setText(String.valueOf(selectedVehicle.getCurrentOdometer()));
            tvVehicleFormMode.setText(getString(R.string.maintenance_vehicle_editing_selected));
            btnSaveVehicle.setText(getString(R.string.maintenance_vehicle_update_button));
        });

        btnDeleteVehicle.setOnClickListener(v -> {
            int position = spinnerExisting.getSelectedItemPosition();
            if (position <= 0) {
                Toast.makeText(requireContext(), getString(R.string.maintenance_error_select_vehicle_delete), Toast.LENGTH_SHORT).show();
                return;
            }

            Vehicle selectedVehicle = allVehicles.get(position - 1);
            executorService.execute(() -> {
                int tripUsageCount = database.vehicleDao().countTripsUsingVehicle(selectedVehicle.getVehicleId());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.maintenance_dialog_delete_vehicle_title)
                            .setMessage(getString(
                                    R.string.maintenance_dialog_delete_vehicle_message_format,
                                    selectedVehicle.getLicensePlate(),
                                    tripUsageCount
                            ))
                            .setPositiveButton(R.string.common_delete, (d, which) -> deleteVehicle(selectedVehicle, dialog))
                            .setNegativeButton(R.string.common_cancel, null)
                            .show());
                }
            });
        });

        btnSaveVehicle.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            String initialOdoStr = etInitialOdo.getText().toString().trim();
            String currentOdoStr = etCurrentOdo.getText().toString().trim();

            clearErrors(etPlate, etInitialOdo, etCurrentOdo);

            if (ValidationUtils.isNullOrEmpty(plate)) {
                showFieldError(etPlate, getString(R.string.maintenance_error_vehicle_plate_required));
                return;
            }
            if (!ValidationUtils.isValidVehiclePlate(plate)) {
                showFieldError(etPlate, getString(R.string.maintenance_error_vehicle_plate_format));
                return;
            }
            for (Vehicle existingVehicle : allVehicles) {
                if (editingVehicle[0] != null && existingVehicle.getVehicleId() == editingVehicle[0].getVehicleId()) {
                    continue;
                }
                if (existingVehicle.getLicensePlate() != null
                        && existingVehicle.getLicensePlate().equalsIgnoreCase(plate)) {
                    showFieldError(etPlate, getString(R.string.maintenance_error_vehicle_plate_exists));
                    return;
                }
            }
            if (!ValidationUtils.isIntegerInRange(initialOdoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etInitialOdo, getString(R.string.maintenance_error_initial_odo_invalid));
                return;
            }
            if (!ValidationUtils.isIntegerInRange(currentOdoStr, MIN_ODOMETER, MAX_ODOMETER)) {
                showFieldError(etCurrentOdo, getString(R.string.maintenance_error_current_odo_invalid));
                return;
            }

            int initialOdo = Integer.parseInt(initialOdoStr);
            int currentOdo = Integer.parseInt(currentOdoStr);
            if (currentOdo < initialOdo) {
                showFieldError(etCurrentOdo, getString(R.string.maintenance_error_current_odo_less_than_initial));
                return;
            }

            executorService.execute(() -> {
                if (editingVehicle[0] != null) {
                    Vehicle vehicleToUpdate = database.vehicleDao().getVehicleById(editingVehicle[0].getVehicleId());
                    if (vehicleToUpdate == null) {
                        showToastOnUiThread(getString(R.string.maintenance_toast_vehicle_not_found_update));
                        return;
                    }

                    vehicleToUpdate.setLicensePlate(plate.toUpperCase(Locale.getDefault()));
                    vehicleToUpdate.setInitialOdometer(initialOdo);
                    vehicleToUpdate.setCurrentOdometer(currentOdo);
                    database.vehicleDao().update(vehicleToUpdate);
                } else {
                    Vehicle newVehicle = new Vehicle(plate.toUpperCase(Locale.getDefault()), initialOdo, currentOdo);
                    int newVehicleId = (int) database.vehicleDao().insert(newVehicle);
                    newVehicle.setVehicleId(newVehicleId);

                    if (activeTrip != null) {
                        Trip latestTrip = database.tripDao().getTripById(activeTrip.getId());
                        if (latestTrip != null) {
                            latestTrip.setVehicleId(newVehicleId);
                            database.tripDao().update(latestTrip);
                        }
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), editingVehicle[0] != null
                                ? getString(R.string.maintenance_toast_vehicle_updated)
                                : activeTrip != null
                                ? getString(R.string.maintenance_toast_vehicle_added_and_assigned)
                                : getString(R.string.maintenance_toast_vehicle_added), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        });

        dialog.show();
    }

    private void showManageIntervalsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.maintenance_intervals_dialog_title)
                .setPositiveButton(R.string.maintenance_intervals_add_type_button, (dialog, which) -> showAddMaintenanceTypeDialog())
                .setNegativeButton(R.string.common_close, null);

        if (maintenanceTypes.isEmpty()) {
            builder.setMessage(R.string.maintenance_intervals_empty_message);
        } else {
            String[] typeNames = new String[maintenanceTypes.size()];
            for (int i = 0; i < maintenanceTypes.size(); i++) {
                MaintenanceType type = maintenanceTypes.get(i);
                typeNames[i] = getString(R.string.maintenance_interval_item_format, type.getName(), type.getIntervalKm());
            }
            builder.setItems(typeNames, (dialog, which) -> showEditIntervalDialog(maintenanceTypes.get(which)));
        }

        builder.show();
    }

    private void showEditIntervalDialog(MaintenanceType maintenanceType) {
        EditText input = new EditText(requireContext());
        input.setText(String.valueOf(maintenanceType.getIntervalKm()));
        input.setHint(getString(R.string.maintenance_interval_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(dp(20), dp(20), dp(20), dp(20));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.maintenance_interval_dialog_title_format, maintenanceType.getName()))
                .setView(input)
                .setPositiveButton(R.string.common_save, null)
                .setNegativeButton(R.string.common_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String intervalStr = input.getText().toString().trim();
            if (!ValidationUtils.isIntegerInRange(intervalStr, 1, MAX_INTERVAL_KM)) {
                input.setError(getString(R.string.maintenance_error_interval_positive_integer));
                return;
            }

            int newInterval = Integer.parseInt(intervalStr);
            executorService.execute(() -> {
                maintenanceType.setIntervalKm(newInterval);
                database.maintenanceDao().updateType(maintenanceType);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_interval_updated), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        }));

        dialog.show();
    }

    private void showAddMaintenanceTypeDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        EditText etName = new EditText(requireContext());
        etName.setHint(getString(R.string.maintenance_type_name_hint));
        layout.addView(etName);

        EditText etInterval = new EditText(requireContext());
        etInterval.setHint(getString(R.string.maintenance_interval_hint));
        etInterval.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etInterval);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.maintenance_add_type_dialog_title)
                .setView(layout)
                .setPositiveButton(R.string.common_save, null)
                .setNegativeButton(R.string.common_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String intervalStr = etInterval.getText().toString().trim();

            if (ValidationUtils.isNullOrEmpty(name)) {
                etName.setError(getString(R.string.maintenance_error_type_name_required));
                return;
            }
            if (!ValidationUtils.isValidDisplayName(name, MIN_NAME_LENGTH, MAX_NAME_LENGTH)) {
                etName.setError(getString(R.string.maintenance_error_type_name_invalid));
                return;
            }
            if (ValidationUtils.isDuplicateName(name, getMaintenanceTypeNames())) {
                etName.setError(getString(R.string.maintenance_error_type_exists));
                return;
            }
            if (!ValidationUtils.isIntegerInRange(intervalStr, 1, MAX_INTERVAL_KM)) {
                etInterval.setError(getString(R.string.maintenance_error_interval_positive_integer));
                return;
            }

            int interval = Integer.parseInt(intervalStr);
            executorService.execute(() -> {
                database.maintenanceDao().insertType(new MaintenanceType(name, interval));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.maintenance_toast_type_added), Toast.LENGTH_SHORT).show();
                        loadMaintenanceData();
                    });
                }
            });
        }));

        dialog.show();
    }

    @Nullable
    private MaintenanceType findOilType(List<MaintenanceType> types) {
        for (MaintenanceType type : types) {
            if (type.getName() != null && type.getName().toLowerCase().contains("nhớt")) {
                return type;
            }
        }
        return null;
    }

    private FuelStats calculateFuelStats(List<FuelLog> fuelLogs, int baselineOdo) {
        FuelStats stats = new FuelStats();
        stats.baselineOdo = baselineOdo;
        if (fuelLogs == null || fuelLogs.isEmpty()) {
            return stats;
        }

        List<FuelLog> scopedLogs = new ArrayList<>();
        for (FuelLog fuelLog : fuelLogs) {
            if (fuelLog.getOdoReading() >= baselineOdo) {
                scopedLogs.add(fuelLog);
            }
        }

        stats.logCount = scopedLogs.size();
        if (scopedLogs.isEmpty()) {
            return stats;
        }

        double totalDistance = 0;
        double litersUsed = 0;
        int segmentStartOdo = baselineOdo;
        for (FuelLog current : scopedLogs) {
            stats.totalLiters += current.getLiters();
            int distance = current.getOdoReading() - segmentStartOdo;
            if (distance > 0 && current.getLiters() > 0) {
                totalDistance += distance;
                litersUsed += current.getLiters();
                stats.hasLatestSegment = true;
                stats.latestSegmentStartOdo = segmentStartOdo;
                stats.latestFuelOdo = current.getOdoReading();
                stats.latestSegmentDistance = distance;
                stats.latestSegmentLiters = current.getLiters();
                stats.latestSegmentKmPerLiter = distance / current.getLiters();
                stats.latestSegmentLPer100 = (current.getLiters() / distance) * 100;
            }
            segmentStartOdo = Math.max(segmentStartOdo, current.getOdoReading());
        }

        stats.totalDistance = totalDistance;
        if (totalDistance > 0 && litersUsed > 0) {
            stats.averageConsumptionLPer100 = (litersUsed / totalDistance) * 100;
            stats.kmPerLiter = totalDistance / litersUsed;
        }
        return stats;
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }

    private int getFuelBaselineOdometer(Context context, Vehicle vehicle) {
        int initialBaseline = Math.max(MIN_ODOMETER, vehicle.getInitialOdometer());
        SharedPreferences preferences = context.getSharedPreferences(PREFS_MAINTENANCE, Context.MODE_PRIVATE);
        int saved = preferences.getInt(PREF_KEY_FUEL_BASELINE_PREFIX + vehicle.getVehicleId(), initialBaseline);
        int clampedToCurrent = Math.min(saved, vehicle.getCurrentOdometer());
        return Math.max(initialBaseline, clampedToCurrent);
    }

    private void saveFuelBaselineOdometer(Context context, int vehicleId, int baselineOdo) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_MAINTENANCE, Context.MODE_PRIVATE);
        preferences.edit().putInt(PREF_KEY_FUEL_BASELINE_PREFIX + vehicleId, baselineOdo).apply();
    }

    private void deleteVehicle(Vehicle vehicle, Dialog parentDialog) {
        executorService.execute(() -> {
            database.vehicleDao().delete(vehicle);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (parentDialog != null && parentDialog.isShowing()) {
                        parentDialog.dismiss();
                    }
                    Toast.makeText(requireContext(), getString(R.string.maintenance_toast_vehicle_deleted), Toast.LENGTH_SHORT).show();
                    loadMaintenanceData();
                });
            }
        });
    }

    private void showToastOnUiThread(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    private List<String> getMaintenanceTypeNames() {
        List<String> names = new ArrayList<>();
        for (MaintenanceType type : maintenanceTypes) {
            names.add(type.getName());
        }
        return names;
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

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private static class FuelStats {
        private int baselineOdo;
        private double averageConsumptionLPer100;
        private double kmPerLiter;
        private double totalLiters;
        private double totalDistance;
        private boolean hasLatestSegment;
        private int latestSegmentStartOdo;
        private int latestFuelOdo;
        private double latestSegmentDistance;
        private double latestSegmentLiters;
        private double latestSegmentKmPerLiter;
        private double latestSegmentLPer100;
        private int logCount;
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
