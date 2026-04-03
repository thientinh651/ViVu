package com.tinh.vivu.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses",
        foreignKeys = {
                // Ràng buộc khóa ngoại với bảng Trip (Nếu xóa Trip, các Expense của Trip đó cũng bị xóa)
                @ForeignKey(entity = Trip.class,
                        parentColumns = "id",
                        childColumns = "tripId",
                        onDelete = ForeignKey.CASCADE),
                // Ràng buộc khóa ngoại với bảng ExpenseCategory
                @ForeignKey(entity = ExpenseCategory.class,
                        parentColumns = "categoryId",
                        childColumns = "categoryId",
                        onDelete = ForeignKey.RESTRICT)
        },
        // Đánh index cho khóa ngoại để tăng tốc độ truy vấn
        indices = {@Index("tripId"), @Index("categoryId")}
)
public class Expense {
    @PrimaryKey(autoGenerate = true)
    private int expenseId;

    // Dùng Integer thay vì int để cho phép giá trị null (vì chọn chuyến đi là ko bắt buoc)
    private Integer tripId;

    private int categoryId;
    private double amount;
    private String expenseDate;
    private String note;

    public Expense(Integer tripId, int categoryId, double amount, String expenseDate, String note) {
        this.tripId = tripId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.note = note;
    }

    // --- Getters & Setters ---
    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }

    public Integer getTripId() { return tripId; }
    public void setTripId(Integer tripId) { this.tripId = tripId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}