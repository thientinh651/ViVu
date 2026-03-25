package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.ChecklistCategory;
import com.tinh.vivu.models.ChecklistTask;

import java.util.List;

@Dao
public interface ChecklistDao {

    // --- Thao tác với Danh mục (ChecklistCategory) ---
    @Insert
    void insertCategory(ChecklistCategory category);

    @Update
    void updateCategory(ChecklistCategory category); // Hàm Cập nhật (Đổi tên) danh mục

    @Query("SELECT * FROM checklist_categories WHERE tripId = :tripId")
    List<ChecklistCategory> getCategoriesByTripId(int tripId);

    @Delete
    void deleteCategory(ChecklistCategory category);

    // --- Thao tác với Nhiệm vụ (ChecklistTask) ---
    @Insert
    void insertTask(ChecklistTask task);

    @Update
    void updateTask(ChecklistTask task);

    @Delete
    void deleteTask(ChecklistTask task);

    @Query("SELECT * FROM checklist_tasks WHERE categoryId = :categoryId")
    List<ChecklistTask> getTasksByCategoryId(int categoryId);

    @Query("SELECT * FROM checklist_tasks WHERE tripId = :tripId")
    List<ChecklistTask> getAllTasksByTripId(int tripId);

    // Đưa TẤT CẢ mục trong Trip về chưa hoàn thành
    @Query("UPDATE checklist_tasks SET isCompleted = 0, completedAt = 0 WHERE tripId = :tripId")
    void resetChecklist(int tripId);

    // Đưa các mục của RIÊNG 1 DANH MỤC về chưa hoàn thành
    @Query("UPDATE checklist_tasks SET isCompleted = 0, completedAt = 0 WHERE categoryId = :categoryId")
    void resetChecklistByCategory(int categoryId);

    // Xóa tất cả các Item bên trong khi người dùng Xóa 1 danh mục
    @Query("DELETE FROM checklist_tasks WHERE categoryId = :categoryId")
    void deleteTasksByCategoryId(int categoryId);
}