package com.tinh.vivu;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tinh.vivu.fragments.ChecklistFragment;
import com.tinh.vivu.fragments.ExpenseFragment;
import com.tinh.vivu.fragments.HomeFragment;
import com.tinh.vivu.fragments.MusicFragment;
import com.tinh.vivu.fragments.MoreFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Áp dụng khoảng đệm (padding) TRỰC TIẾP lên thanh Menu.
        // Tự động đẩy các icon lên cao hơn thanh điều hướng ảo của máy.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // Mặc định nạp Fragment Home khi mở app để phần thân giữa không bị trắng
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();

            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_checklist) {
                selectedFragment = new ChecklistFragment();
            } else if (itemId == R.id.nav_expense) {
                selectedFragment = new ExpenseFragment();
            } else if (itemId == R.id.nav_music) {
                selectedFragment = new MusicFragment();
            } else if (itemId == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }


            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent, false);
    }

    // Hàm xử lý điều hướng chung
    private void handleNavigationIntent(Intent intent, boolean isFirstLaunch) {
        if (intent != null && intent.hasExtra("navigate_to")) {
            String navigateTo = intent.getStringExtra("navigate_to");
            if ("music".equals(navigateTo)) {
                // Chuyển sang tab Music
                bottomNavigationView.setSelectedItemId(R.id.nav_music);
                return; // Thoát sớm, không chạy logic bên dưới
            }
        }

        // Nếu là lần đầu tiên mở ứng dụng (không phải do click từ thông báo nhạc), chọn trang mặc định là Home
        if (isFirstLaunch) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}