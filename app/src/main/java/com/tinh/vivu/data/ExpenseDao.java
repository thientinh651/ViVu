package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    // Lấy TẤT CẢ khoản chi (Sắp xếp mới nhất lên đầu)
    @Query("SELECT * FROM expenses ORDER BY expenseId DESC")
    List<Expense> getAllExpenses();

    // Lấy các khoản chi theo 1 chuyến đi cụ thể
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY expenseId DESC")
    List<Expense> getExpensesByTripId(int tripId);

    // Tính TỔNG CHI TIÊU của tất cả chuyến đi
    @Query("SELECT SUM(amount) FROM expenses")
    double getTotalSpentAll();

    // Tính TỔNG CHI TIÊU của 1 chuyến đi cụ thể
    @Query("SELECT SUM(amount) FROM expenses WHERE tripId = :tripId")
    double getTotalSpentByTripId(int tripId);

    // Tính tổng chi tiêu THEO TỪNG DANH MỤC (Ví dụ: Tổng tiền đổ Xăng) cho TẤT CẢ chuyến đi
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId")
    double getTotalSpentByCategoryAll(int categoryId);

    // Tính tổng chi tiêu THEO TỪNG DANH MỤC cho 1 CHUYẾN ĐI cụ thể
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND tripId = :tripId")
    double getTotalSpentByCategoryAndTrip(int categoryId, int tripId);
}