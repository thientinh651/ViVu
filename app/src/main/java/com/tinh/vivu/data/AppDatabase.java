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
    public abstract ExpenseCategoryDao expenseCategoryDao();
    public abstract ExpenseDao expenseDao();
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
                ChecklistDao checklistDao = instance.checklistDao();
                ExpenseCategoryDao expenseCategoryDao = instance.expenseCategoryDao();
                ExpenseDao expenseDao = instance.expenseDao();
                SongDao songDao = instance.songDao();
                PlayListDao playListDao = instance.playListDao();


                Trip trip1 = new Trip("Khám phá Tây Bắc", "01/01/2026", "29/06/2026", 5000000, "Planning");
                Trip trip2 = new Trip("Hà Nội - Đà Nẵng", "01/07/2026", "25/12/2026", 15000000, "Ongoing");
                tripDao.insert(trip1);
                tripDao.insert(trip2);


                routeStopDao.insert(new RouteStop(1, "Hà Nội (Điểm xuất phát)", 1, "06:00 01/12", "07:00 01/12"));
                routeStopDao.insert(new RouteStop(1, "Hòa Bình (Nghỉ chân)", 2, "10:00 01/12", "11:30 01/12"));
                routeStopDao.insert(new RouteStop(1, "Sơn La (Nghỉ đêm)", 3, "17:00 01/12", "08:00 02/12"));


                // Danh mục (Categories)
                checklistDao.insertCategory(new ChecklistCategory(1, "Giấy tờ & Tiền")); // ID 1
                checklistDao.insertCategory(new ChecklistCategory(1, "Đồ dùng cá nhân")); // ID 2
                checklistDao.insertCategory(new ChecklistCategory(1, "Đồ bảo hộ xe"));   // ID 3

                // Công việc (Tasks) -
                checklistDao.insertTask(new ChecklistTask(1, 1, "Căn cước công dân"));
                checklistDao.insertTask(new ChecklistTask(1, 1, "Bằng lái xe & Đăng ký xe"));
                checklistDao.insertTask(new ChecklistTask(1, 1, "Tiền mặt (2 triệu)"));

                checklistDao.insertTask(new ChecklistTask(2, 1, "Quần áo ấm"));
                checklistDao.insertTask(new ChecklistTask(2, 1, "Bàn chải & Kem đánh răng"));

                checklistDao.insertTask(new ChecklistTask(3, 1, "Mũ bảo hiểm fullface"));
                checklistDao.insertTask(new ChecklistTask(3, 1, "Giáp tay chân"));
                checklistDao.insertTask(new ChecklistTask(3, 1, "Bộ dụng cụ sửa xe mini"));


                expenseCategoryDao.insert(new ExpenseCategory("Xăng xe")); // ID 1
                expenseCategoryDao.insert(new ExpenseCategory("Ăn uống"));  // ID 2
                expenseCategoryDao.insert(new ExpenseCategory("Lưu trú"));   // ID 3
                expenseCategoryDao.insert(new ExpenseCategory("Khác"));      // ID 4

                expenseDao.insert(new Expense(1, 1, 150000, "01/2/2026", "Đổ xăng tại Hà Nội"));
                expenseDao.insert(new Expense(1, 2, 80000, "01/3/2026", "Ăn sáng phở bò"));
                expenseDao.insert(new Expense(1, 3, 350000, "01/4/2026", "Homestay tại Sơn La"));


                playListDao.insert(new PlayList("Nhạc Đi Phượt"));
                playListDao.insert(new PlayList("Nhạc Thư Giãn"));
            });
        }
    };
}