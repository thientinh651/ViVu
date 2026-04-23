package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expense_categories")
public class ExpenseCategory {
    @PrimaryKey(autoGenerate = true)
    private int categoryId;

    private String categoryName;

    public ExpenseCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String toString() {
        return categoryName;
    }
}