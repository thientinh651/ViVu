package com.tinh.vivu.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tinh.vivu.R;

public class ToastUtils {

    /**
     * Hàm hiển thị Toast với Logo tùy chỉnh
     * @param context Ngữ cảnh hiện tại (Ví dụ: MainActivity.this)
     * @param message Lời nhắn muốn hiển thị
     * @param iconResId ID của hình ảnh/logo (Ví dụ: R.drawable.ic_check_circle)
     */
    public static void showCustomToast(Context context, String message, int iconResId) {
        // Nạp giao diện custom_toast.xml
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        // Ánh xạ các view bên trong
        ImageView image = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        // Đặt dữ liệu
        image.setImageResource(iconResId);
        text.setText(message);

        // Tạo và hiển thị Toast
        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}