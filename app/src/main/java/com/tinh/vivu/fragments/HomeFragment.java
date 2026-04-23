package com.tinh.vivu.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
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
import com.tinh.vivu.TripDetailActivity;
import com.tinh.vivu.data.AppDatabase;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.utils.ValidationUtils;
import com.tinh.vivu.views.TripAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

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
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa chuyến đi này không?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            executorService.execute(() -> {
                                database.tripDao().delete(trip);
                                loadTrips();
                            });
                        })
                        .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onEditClick(Trip trip) {
                showTripDialog(trip);
            }

            @Override
            public void onTripClick(Trip trip) {
                Intent intent = new Intent(requireActivity(), TripDetailActivity.class);
                intent.putExtra("TRIP_ID", trip.getId());
                startActivity(intent);
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
        MaterialButton btnAction = dialog.findViewById(R.id.btn_create_trip);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        if (tripToEdit != null) {
            if (tvTitle != null) tvTitle.setText("Chỉnh Sửa Chuyến Đi");
            btnAction.setText("Cập nhật");
            etName.setText(tripToEdit.getName());
            etStart.setText(tripToEdit.getStartDate());
            etEnd.setText(tripToEdit.getEndDate());
            etBudget.setText(tripToEdit.getTotalBudget() == 0 ? "" : String.valueOf((long)tripToEdit.getTotalBudget()));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        etStart.setOnClickListener(v -> showDatePicker(etStart));
        etEnd.setOnClickListener(v -> showDatePicker(etEnd));

        btnAction.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String start = etStart.getText().toString().trim();
            String end = etEnd.getText().toString().trim();
            String budgetStr = etBudget.getText().toString().trim();

            if (ValidationUtils.isNullOrEmpty(name)) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên chuyến đi!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ValidationUtils.isNullOrEmpty(start)) {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!ValidationUtils.isAfterOrEqualCurrentDate(start, "dd/MM/yyyy")) {
                Toast.makeText(requireContext(), "Ngày bắt đầu không được chọn ngày quá khứ!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!ValidationUtils.isNullOrEmpty(end)) {
                if (!ValidationUtils.isStartBeforeOrEqualEnd(start, end, "dd/MM/yyyy")) {
                    Toast.makeText(requireContext(), "Ngày kết thúc phải sau hoặc trùng ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double tempBudget = 0;
            if (!ValidationUtils.isNullOrEmpty(budgetStr)) {
                if (ValidationUtils.isNumeric(budgetStr)) {
                    tempBudget = Double.parseDouble(budgetStr);
                } else {
                    Toast.makeText(requireContext(), "Ngân sách phải là một con số!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            final double finalBudget = tempBudget;

            executorService.execute(() -> {
                if (tripToEdit == null) {
                    Trip newTrip = new Trip(name, start, end, finalBudget, "Lên kế hoạch");
                    database.tripDao().insert(newTrip);
                } else {
                    tripToEdit.setName(name);
                    tripToEdit.setStartDate(start);
                    tripToEdit.setEndDate(end);
                    tripToEdit.setTotalBudget(finalBudget);
                    database.tripDao().update(tripToEdit);
                }

                loadTrips();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), tripToEdit == null ? "Tạo chuyến đi thành công!" : "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
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
}