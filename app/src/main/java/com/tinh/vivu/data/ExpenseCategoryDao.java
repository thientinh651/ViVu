package com.tinh.vivu.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tinh.vivu.models.ExpenseCategory;

import java.util.List;

@Dao
public interface ExpenseCategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ExpenseCategory category);

    @Update
    void update(ExpenseCategory category);

    @Delete
    void delete(ExpenseCategory category);

    @Query("SELECT * FROM expense_categories")
    List<ExpenseCategory> getAllCategories();
}