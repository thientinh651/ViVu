package com.tinh.vivu.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tinh.vivu.models.Alarm;
import com.tinh.vivu.models.ChecklistCategory;
import com.tinh.vivu.models.ChecklistTask;
import com.tinh.vivu.models.Expense;
import com.tinh.vivu.models.ExpenseCategory;
import com.tinh.vivu.models.FuelLog;
import com.tinh.vivu.models.JourneyLog;
import com.tinh.vivu.models.MaintenanceLog;
import com.tinh.vivu.models.MaintenanceType;
import com.tinh.vivu.models.PlayList;
import com.tinh.vivu.models.PlayListSong;
import com.tinh.vivu.models.RouteStop;
import com.tinh.vivu.models.Song;
import com.tinh.vivu.models.Trip;
import com.tinh.vivu.models.Vehicle;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        Vehicle.class,
        Trip.class, RouteStop.class,
        ChecklistCategory.class, ChecklistTask.class,
        ExpenseCategory.class, Expense.class,
        MaintenanceType.class, MaintenanceLog.class, FuelLog.class,
        Song.class, PlayList.class, PlayListSong.class,
        JourneyLog.class, Alarm.class
}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TripDao tripDao();
    public abstract RouteStopDao routeStopDao();
    public abstract ChecklistDao checklistDao();
    public abstract ExpenseCategoryDao expenseCategoryDao();
    public abstract ExpenseDao expenseDao();
    public abstract VehicleDao vehicleDao();
    public abstract MaintenanceDao maintenanceDao();
    public abstract FuelLogDao fuelLogDao();
    public abstract SongDao songDao();
    public abstract PlayListDao playListDao();
    public abstract PlayListSongDao playListSongDao();
    public abstract JourneyLogDao journeyLogDao();
    public abstract AlarmDao alarmDao();


    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "vivu_database")
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_9_10)
                    .addCallback(roomCallback)
                    .build();
        }
        return instance;
    }

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `checklist_tasks_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`categoryId` INTEGER NOT NULL, " +
                    "`name` TEXT, " +
                    "`isCompleted` INTEGER NOT NULL, " +
                    "`completedAt` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)");
            database.execSQL("INSERT INTO checklist_tasks_new (id, categoryId, name, isCompleted, completedAt, createdAt) " +
                    "SELECT id, categoryId, name, isCompleted, completedAt, createdAt FROM checklist_tasks");
            database.execSQL("DROP TABLE checklist_tasks");
            database.execSQL("ALTER TABLE checklist_tasks_new RENAME TO checklist_tasks");
        }
    };

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                TripDao tripDao = instance.tripDao();
                RouteStopDao routeStopDao = instance.routeStopDao();
                ChecklistDao checklistDao = instance.checklistDao();
                ExpenseCategoryDao expenseCategoryDao = instance.expenseCategoryDao();
                ExpenseDao expenseDao = instance.expenseDao();
                VehicleDao vehicleDao = instance.vehicleDao();
                MaintenanceDao maintenanceDao = instance.maintenanceDao();
                FuelLogDao fuelLogDao = instance.fuelLogDao();
                PlayListDao playListDao = instance.playListDao();
                AlarmDao alarmDao = instance.alarmDao();


                // Dữ liệu mẫu ban đầu của bạn
                Trip trip1 = new Trip("Khám phá Tây Bắc", "01/01/2026", "29/06/2026", 5000000, "Planning");
                Trip trip2 = new Trip("Hà Nội - Đà Nẵng", "01/07/2026", "25/12/2026", 15000000, "Ongoing");
                int trip1Id = (int) tripDao.insert(trip1);
                int trip2Id = (int) tripDao.insert(trip2);
                trip1.setId(trip1Id);
                trip2.setId(trip2Id);

                Vehicle bike = new Vehicle("49K1-362.82", 75000, 75000);
                int vehicleId = (int) vehicleDao.insert(bike);
                bike.setVehicleId(vehicleId);

                trip1.setVehicleId(vehicleId);
                trip2.setVehicleId(vehicleId);
                tripDao.update(trip1);
                tripDao.update(trip2);

                routeStopDao.insert(new RouteStop(trip1Id, "Hà Nội (Điểm xuất phát)", 1, "06:00 01/12", "07:00 01/12"));
                routeStopDao.insert(new RouteStop(trip1Id, "Hòa Bình (Nghỉ chân)", 2, "10:00 01/12", "11:30 01/12"));
                routeStopDao.insert(new RouteStop(trip1Id, "Sơn La (Nghỉ đêm)", 3, "17:00 01/12", "08:00 02/12"));

                checklistDao.insertCategory(new ChecklistCategory(trip1Id, "Giấy tờ & Tiền"));
                checklistDao.insertCategory(new ChecklistCategory(trip1Id, "Đồ dùng cá nhân"));
                checklistDao.insertCategory(new ChecklistCategory(trip1Id, "Đồ bảo hộ xe"));

                checklistDao.insertTask(new ChecklistTask(1, "Căn cước công dân"));
                checklistDao.insertTask(new ChecklistTask(1, "Bằng lái xe & Đăng ký xe"));
                checklistDao.insertTask(new ChecklistTask(1, "Tiền mặt (2 triệu)"));
                checklistDao.insertTask(new ChecklistTask(2, "Quần áo ấm"));
                checklistDao.insertTask(new ChecklistTask(2, "Bàn chải & Kem đánh răng"));
                checklistDao.insertTask(new ChecklistTask(3, "Mũ bảo hiểm fullface"));
                checklistDao.insertTask(new ChecklistTask(3, "Giáp tay chân"));
                checklistDao.insertTask(new ChecklistTask(3, "Bộ dụng cụ sửa xe mini"));

                expenseCategoryDao.insert(new ExpenseCategory("Xăng xe"));
                expenseCategoryDao.insert(new ExpenseCategory("Ăn uống"));
                expenseCategoryDao.insert(new ExpenseCategory("Lưu trú"));
                expenseCategoryDao.insert(new ExpenseCategory("Khác"));

                expenseDao.insert(new Expense(trip1Id, 1, 150000, "01/2/2026", "Đổ xăng tại Hà Nội"));
                expenseDao.insert(new Expense(trip1Id, 2, 80000, "01/3/2026", "Ăn sáng phở bò"));
                expenseDao.insert(new Expense(trip1Id, 3, 350000, "01/4/2026", "Homestay tại Sơn La"));

                int oilTypeId = (int) maintenanceDao.insertType(new MaintenanceType("Thay nhớt", 3000));
                int chainTypeId = (int) maintenanceDao.insertType(new MaintenanceType("Bôi trơn sên", 500));
                int brakeTypeId = (int) maintenanceDao.insertType(new MaintenanceType("Kiểm tra phanh", 1000));
                int airFilterTypeId = (int) maintenanceDao.insertType(new MaintenanceType("Vệ sinh lọc gió", 2000));

                maintenanceDao.insertLog(new MaintenanceLog(vehicleId, oilTypeId, 1200, 180000, "15/02/2026", "Thay nhớt định kỳ"));
                maintenanceDao.insertLog(new MaintenanceLog(vehicleId, chainTypeId, 2400, 50000, "10/03/2026", "Bôi trơn và kiểm tra sên"));
                maintenanceDao.insertLog(new MaintenanceLog(vehicleId, brakeTypeId, 1800, 120000, "05/03/2026", "Vệ sinh và chỉnh phanh trước"));
                maintenanceDao.insertLog(new MaintenanceLog(vehicleId, airFilterTypeId, 1100, 70000, "25/01/2026", "Vệ sinh lọc gió"));

                fuelLogDao.insert(new FuelLog(trip2Id, vehicleId, 1600, 6.0, 23500, 141000, "Hà Nội", "02/03/2026"));
                fuelLogDao.insert(new FuelLog(trip2Id, vehicleId, 1950, 12.0, 23800, 285600, "Thanh Hóa", "08/03/2026"));
                fuelLogDao.insert(new FuelLog(trip2Id, vehicleId, 2450, 17.5, 24000, 420000, "Đà Nẵng", "15/03/2026"));

                playListDao.insert(new PlayList("Nhạc Đi Phượt"));
                playListDao.insert(new PlayList("Nhạc Thư Giãn"));

                boolean isVietnamese = Locale.getDefault().getLanguage().startsWith("vi");
                String wakeLabel = isVietnamese ? "Thức dậy" : "Wake up";
                String fitnessLabel = isVietnamese ? "Tập thể dục" : "Fitness";
                String studyLabel = isVietnamese ? "Học tập" : "Study";
                String lunchLabel = isVietnamese ? "Ăn trưa" : "Lunch";

                alarmDao.insert(new Alarm(6, 0, wakeLabel, "Everyday", false));
                alarmDao.insert(new Alarm(7, 0, fitnessLabel, "Everyday", false));
                alarmDao.insert(new Alarm(8, 45, studyLabel, "Mon, Tue, Fri", false));
                alarmDao.insert(new Alarm(12, 30, lunchLabel, "Mon, Tue, Fri", false));
            });
        }
    };
}