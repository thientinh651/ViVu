package com.tinh.vivu.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Trip;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Trip.class, RouteStop.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TripDao tripDao();
    public abstract RouteStopDao routeStopDao();

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "vivu_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback) // Gọi hàm Callback để tạo dữ liệu mẫu
                    .build();
        }
        return instance;
    }

    // Lắng nghe sự kiện lần đầu tiên tạo Database
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Chạy ngầm để nạp dữ liệu Demo
            databaseWriteExecutor.execute(() -> {
                TripDao tripDao = instance.tripDao();
                RouteStopDao routeStopDao = instance.routeStopDao();

                // 1. NẠP 3 CHUYẾN ĐI DEMO
                Trip trip1 = new Trip("Khám phá Tây Bắc", "01/12/2024", "10/12/2024", 5000000, "Upcoming");
                Trip trip2 = new Trip("Phượt Đà Lạt", "15/01/2025", "20/01/2025", 3000000, "Planning");
                Trip trip3 = new Trip("Vòng quanh Miền Tây", "05/04/2025", "15/04/2025", 7000000, "Completed");

                tripDao.insert(trip1);
                tripDao.insert(trip2);
                tripDao.insert(trip3);

                // 2. NẠP 3 CHẶNG DỪNG DEMO CHO CHUYẾN ĐI SỐ 1 (Tây Bắc) - DÙNG SỐ DOUBLE
                // tripId = 1
                routeStopDao.insert(new RouteStop(1, "Hà Nội", 1, 0.0));
                routeStopDao.insert(new RouteStop(1, "Hòa Bình", 2, 70.5));
                routeStopDao.insert(new RouteStop(1, "Mộc Châu", 3, 200.0));
            });
        }
    };
}