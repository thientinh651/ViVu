package com.tinh.vivu.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tinh.vivu.models.ChecklistCategory;
import com.tinh.vivu.models.ChecklistTask;
import com.tinh.vivu.models.Expense;
import com.tinh.vivu.models.ExpenseCategory;
import com.tinh.vivu.models.PlayList;
import com.tinh.vivu.models.PlayListSong;
import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Song;
import com.tinh.vivu.models.Trip;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Cập nhật mảng entities thêm Song, PlayList, PlayListSong. Tăng version lên 6
@Database(entities = {
        Trip.class, RouteStop.class,
        ChecklistCategory.class, ChecklistTask.class,
        ExpenseCategory.class, Expense.class,
        Song.class, PlayList.class, PlayListSong.class
}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TripDao tripDao();
    public abstract RouteStopDao routeStopDao();
    public abstract ChecklistDao checklistDao();

    // Thêm DAO cho Quản lý chi tiêu
    public abstract ExpenseCategoryDao expenseCategoryDao();
    public abstract ExpenseDao expenseDao();

    // Thêm DAO cho tính năng Nghe nhạc
    public abstract SongDao songDao();
    public abstract PlayListDao playListDao();
    public abstract PlayListSongDao playListSongDao();

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "vivu_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build();
        }
        return instance;
    }

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                TripDao tripDao = instance.tripDao();
                RouteStopDao routeStopDao = instance.routeStopDao();
                ExpenseCategoryDao expenseCategoryDao = instance.expenseCategoryDao();

                // 1. NẠP CHUYẾN ĐI MẪU
                Trip trip1 = new Trip("Khám phá Tây Bắc", "01/12/2024", "10/12/2024", 5000000, "Planning");
                Trip trip2 = new Trip("Hà Nội - Đà Nẵng", "11/11/2025", "21/06/2026", 15000000, "Ongoing");
                tripDao.insert(trip1);
                tripDao.insert(trip2);

                // 2. NẠP CHẶNG DỪNG MẪU
                routeStopDao.insert(new RouteStop(1, "Hà Nội", 1, "06:00 01/12", "07:00 01/12"));
                routeStopDao.insert(new RouteStop(1, "Hòa Bình", 2, "10:00 01/12", "11:30 01/12"));

                // 3. NẠP CÁC DANH MỤC CHI TIÊU MẶC ĐỊNH
                expenseCategoryDao.insert(new ExpenseCategory("Xăng xe & Di chuyển"));
                expenseCategoryDao.insert(new ExpenseCategory("Ăn uống"));
                expenseCategoryDao.insert(new ExpenseCategory("Lưu trú (Khách sạn/Homestay)"));
                expenseCategoryDao.insert(new ExpenseCategory("Vé tham quan"));
                expenseCategoryDao.insert(new ExpenseCategory("Mua sắm & Quà cáp"));
                expenseCategoryDao.insert(new ExpenseCategory("Chi phí khác"));
            });
        }
    };
}