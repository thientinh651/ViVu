package com.tinh.vivu.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private ValidationUtils() {
        // Ném ngoại lệ nếu cố tình dùng Reflection để khởi tạo
        throw new UnsupportedOperationException("Đây là lớp tiện ích, không được khởi tạo!");
    }
    // 1. VALIDATION KIỂU CHUỖI (STRING)

    // Kiểm tra chuỗi có bị rỗng hoặc null hay không
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // Kiểm tra độ dài của chuỗi (Min / Max)
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (isNullOrEmpty(str)) return false;
        int len = str.trim().length();
        return len >= minLength && len <= maxLength;
    }

    // Kiểm tra định dạng bằng Regex (Ví dụ: Email, Số điện thoại)
    public static boolean isMatchFormat(String str, String regexPattern) {
        if (isNullOrEmpty(str)) return false;
        return Pattern.compile(regexPattern).matcher(str).matches();
    }

    // Kiểm tra chuỗi chỉ chứa các ký tự cho phép (Ví dụ: Chỉ chữ và số)
    public static boolean isAlphanumeric(String str) {
        return isMatchFormat(str, "^[a-zA-Z0-9]*$");
    }

    // Kiểm tra lặp tên (Kiểm tra xem tên mới có tồn tại trong danh sách cũ chưa)
    public static boolean isDuplicateName(String newName, List<String> existingNames) {
        if (isNullOrEmpty(newName) || existingNames == null) return false;
        for (String name : existingNames) {
            if (name.trim().equalsIgnoreCase(newName.trim())) {
                return true;
            }
        }
        return false;
    }

    // 2. VALIDATION KIỂU SỐ (NUMBER)

    // Kiểm tra xem chuỗi nhập vào có phải là một con số hợp lệ không
    public static boolean isNumeric(String str) {
        if (isNullOrEmpty(str)) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Kiểm tra xem chuỗi có phải là số nguyên (Integer) không
    public static boolean isInteger(String str) {
        if (isNullOrEmpty(str)) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Kiểm tra xem số có nằm trong một khoảng (Range) cho phép không
    public static boolean isNumberInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    // Kiểm tra số dương (> 0)
    public static boolean isPositiveNumber(double value) {
        return value > 0;
    }

    // 3. VALIDATION NGÀY THÁNG (DATE)

    // Kiểm tra ngày tháng có hợp lệ với định dạng không (VD: format = "dd/MM/yyyy")
    public static boolean isValidDate(String dateStr, String format) {
        if (isNullOrEmpty(dateStr)) return false;
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Kiểm tra Ngày Kết Thúc không được bé hơn Ngày Bắt Đầu
    public static boolean isStartBeforeOrEqualEnd(String startDateStr, String endDateStr, String format) {
        if (!isValidDate(startDateStr, format) || !isValidDate(endDateStr, format)) return false;

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            return !startDate.after(endDate);
        } catch (ParseException e) {
            return false;
        }
    }

    // Kiểm tra Ngày Bắt Đầu không được bé hơn ngày hiện tại
    public static boolean isAfterOrEqualCurrentDate(String dateStr, String format) {
        if (!isValidDate(dateStr, format)) return false;
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date dateToCheck = sdf.parse(dateStr);

            Calendar currentCal = Calendar.getInstance();
            currentCal.set(Calendar.HOUR_OF_DAY, 0);
            currentCal.set(Calendar.MINUTE, 0);
            currentCal.set(Calendar.SECOND, 0);
            currentCal.set(Calendar.MILLISECOND, 0);
            Date today = currentCal.getTime();

            return !dateToCheck.before(today);
        } catch (ParseException e) {
            return false;
        }
    }

    // Kiểm tra xem ngày nhập vào có phải hoàn toàn ở Tương Lai không
    public static boolean isFutureDate(String dateStr, String format) {
        if (!isValidDate(dateStr, format)) return false;
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date dateToCheck = sdf.parse(dateStr);
            Date currentDate = new Date();
            return dateToCheck.after(currentDate);
        } catch (ParseException e) {
            return false;
        }
    }

    // 4. VALIDATION TUỔI (AGE)

    // Kiểm tra chuỗi nhập vào có phải là số tuổi hợp lệ (Số nguyên dương)
    public static boolean isValidAgeFormat(String ageStr) {
        if (!isInteger(ageStr)) return false;
        int age = Integer.parseInt(ageStr);
        return age > 0;
    }

    // Kiểm tra tuổi có nằm trong độ tuổi cho phép không
    public static boolean isAgeInRange(String ageStr, int minAge, int maxAge) {
        if (!isValidAgeFormat(ageStr)) return false;
        int age = Integer.parseInt(ageStr);
        return age >= minAge && age <= maxAge;
    }
}