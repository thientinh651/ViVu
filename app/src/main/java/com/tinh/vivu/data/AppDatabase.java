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

@Database(entities = {Trip.class, RouteStop.class}, version = 3)
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

                // 1. NẠP CHUYẾN ĐI MẪU
                Trip trip1 = new Trip("Khám phá Tây Bắc", "01/12/2024", "10/12/2024", 5000000, "Planning");
                Trip trip2 = new Trip("Hà Nội - Đà Nẵng", "11/11/2025", "21/06/2026", 15000000, "Ongoing");

                tripDao.insert(trip1);
                tripDao.insert(trip2);

                // 2. NẠP CHẶNG DỪNG MẪU CHO CHUYẾN ĐI 1 (Tây Bắc)
                // Phải dùng String cho thời gian theo đúng Constructor mới:
                // new RouteStop(tripId, name, orderIndex, expectedArrival, expectedDeparture)
                routeStopDao.insert(new RouteStop(1, "Hà Nội", 1, "06:00 01/12", "07:00 01/12"));
                routeStopDao.insert(new RouteStop(1, "Hòa Bình", 2, "10:00 01/12", "11:30 01/12"));
                routeStopDao.insert(new RouteStop(1, "Mộc Châu", 3, "16:00 01/12", "08:00 02/12"));

                // Demo thêm cho chuyến đi 2
                routeStopDao.insert(new RouteStop(2, "Nghệ An", 1, "12:00 11/11", "13:00 11/11"));
            });
        }
    };
}